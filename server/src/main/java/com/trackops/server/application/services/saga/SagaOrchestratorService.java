package com.trackops.server.application.services.saga;

import com.trackops.server.domain.model.saga.*;
import com.trackops.server.ports.output.persistence.saga.SagaRepository;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import com.trackops.server.domain.model.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SagaOrchestratorService {
    
    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorService.class);
    
    private final SagaRepository sagaRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderSagaStepExecutor orderSagaStepExecutor;

    public SagaOrchestratorService(SagaRepository sagaRepository, 
                                 OrderEventProducer orderEventProducer,
                                 OrderSagaStepExecutor orderSagaStepExecutor) {
        this.sagaRepository = sagaRepository;
        this.orderEventProducer = orderEventProducer;
        this.orderSagaStepExecutor = orderSagaStepExecutor;
    }

    @Transactional
    public UUID startOrderProcessingSaga(UUID orderId) {
        log.info("Starting Order Processing SAGA for order: {}", orderId);
        
        // Create SAGA instance
        SagaInstance sagaInstance = new SagaInstance(
            SagaType.ORDER_PROCESSING, 
            orderId.toString(), 
            3 // max retries
        );
        
        // Define SAGA steps
        addOrderProcessingSteps(sagaInstance, orderId);
        
        // Save SAGA instance
        sagaInstance = sagaRepository.save(sagaInstance);
        
        // Start executing the SAGA
        executeSaga(sagaInstance.getId());
        
        return sagaInstance.getId();
    }

    @Transactional
    public UUID startOrderCancellationSaga(UUID orderId) {
        log.info("Starting Order Cancellation SAGA for order: {}", orderId);
        
        SagaInstance sagaInstance = new SagaInstance(
            SagaType.ORDER_CANCELLATION, 
            orderId.toString(), 
            3
        );
        
        addOrderCancellationSteps(sagaInstance, orderId);
        sagaInstance = sagaRepository.save(sagaInstance);
        
        executeSaga(sagaInstance.getId());
        
        return sagaInstance.getId();
    }

    @Transactional
    public void executeSaga(UUID sagaId) {
        SagaInstance sagaInstance = sagaRepository.findById(sagaId)
            .orElseThrow(() -> new RuntimeException("SAGA instance not found: " + sagaId));
        
        if (sagaInstance.isCompleted()) {
            log.info("SAGA {} is already completed", sagaId);
            return;
        }
        
        try {
            // Execute current step
            if (sagaInstance.getCurrentStepIndex() < sagaInstance.getSteps().size()) {
                SagaStepEntity currentStep = sagaInstance.getSteps().get(sagaInstance.getCurrentStepIndex());
                
                log.info("Executing SAGA step: {} for SAGA: {}", currentStep.getStepName(), sagaId);
                
                // Mark step as in progress
                sagaInstance.markStepInProgress(sagaInstance.getCurrentStepIndex());
                sagaRepository.save(sagaInstance);
                
                // Execute the step
                boolean stepSuccess = orderSagaStepExecutor.executeStep(currentStep, sagaInstance);
                
                if (stepSuccess) {
                    // Mark step as completed
                    sagaInstance.markStepCompleted(sagaInstance.getCurrentStepIndex());
                    sagaRepository.save(sagaInstance);
                    
                    // Check if SAGA is complete
                    if (sagaInstance.getCurrentStepIndex() >= sagaInstance.getSteps().size()) {
                        sagaInstance.markCompleted();
                        sagaRepository.save(sagaInstance);
                        log.info("SAGA {} completed successfully", sagaId);
                    } else {
                        // Continue with next step
                        executeSaga(sagaId);
                    }
                } else {
                    // Step failed, start compensation
                    sagaInstance.markStepFailed(sagaInstance.getCurrentStepIndex(), "Step execution failed");
                    sagaRepository.save(sagaInstance);
                    compensateSaga(sagaId);
                }
            }
        } catch (Exception e) {
            log.error("Error executing SAGA {}: {}", sagaId, e.getMessage(), e);
            sagaInstance.markStepFailed(sagaInstance.getCurrentStepIndex(), e.getMessage());
            sagaRepository.save(sagaInstance);
            compensateSaga(sagaId);
        }
    }

    @Transactional
    public void compensateSaga(UUID sagaId) {
        SagaInstance sagaInstance = sagaRepository.findById(sagaId)
            .orElseThrow(() -> new RuntimeException("SAGA instance not found: " + sagaId));
        
        log.info("Starting compensation for SAGA: {}", sagaId);
        
        sagaInstance.startCompensation();
        sagaRepository.save(sagaInstance);
        
        // Execute compensation steps in reverse order
        for (int i = sagaInstance.getCurrentStepIndex() - 1; i >= 0; i--) {
            SagaStepEntity step = sagaInstance.getSteps().get(i);
            
            if (step.getStatus() == SagaStepStatus.COMPLETED) {
                log.info("Compensating step: {} for SAGA: {}", step.getStepName(), sagaId);
                
                try {
                    boolean compensationSuccess = orderSagaStepExecutor.executeCompensation(step, sagaInstance);
                    
                    if (compensationSuccess) {
                        step.setStatus(SagaStepStatus.COMPENSATED);
                        step.setCompletedAt(Instant.now());
                        sagaRepository.save(sagaInstance);
                    } else {
                        log.error("Compensation failed for step: {} in SAGA: {}", step.getStepName(), sagaId);
                    }
                } catch (Exception e) {
                    log.error("Error compensating step {} in SAGA {}: {}", step.getStepName(), sagaId, e.getMessage(), e);
                }
            }
        }
        
        sagaInstance.markCompensated();
        sagaRepository.save(sagaInstance);
        log.info("SAGA {} compensation completed", sagaId);
    }

    private void addOrderProcessingSteps(SagaInstance sagaInstance, UUID orderId) {
        // Step 1: Validate Order
        sagaInstance.addStep(SagaStep.create(
            "Validate Order",
            "OrderService",
            "validateOrder",
            "cancelOrder",
            orderId
        ));
        
        // Step 2: Reserve Inventory (simulated)
        sagaInstance.addStep(SagaStep.create(
            "Reserve Inventory",
            "InventoryService",
            "reserveInventory",
            "releaseInventory",
            orderId
        ));
        
        // Step 3: Process Payment (simulated)
        sagaInstance.addStep(SagaStep.create(
            "Process Payment",
            "PaymentService",
            "processPayment",
            "refundPayment",
            orderId
        ));
        
        // Step 4: Update Order Status
        sagaInstance.addStep(SagaStep.create(
            "Update Order Status",
            "OrderService",
            "confirmOrder",
            "revertOrderStatus",
            orderId
        ));
        
        // Step 5: Send Notification
        sagaInstance.addStep(SagaStep.create(
            "Send Notification",
            "NotificationService",
            "sendOrderConfirmation",
            "sendOrderCancellation",
            orderId
        ));
    }

    private void addOrderCancellationSteps(SagaInstance sagaInstance, UUID orderId) {
        // Step 1: Cancel Order
        sagaInstance.addStep(SagaStep.create(
            "Cancel Order",
            "OrderService",
            "cancelOrder",
            "restoreOrder",
            orderId
        ));
        
        // Step 2: Release Inventory
        sagaInstance.addStep(SagaStep.create(
            "Release Inventory",
            "InventoryService",
            "releaseInventory",
            "reserveInventory",
            orderId
        ));
        
        // Step 3: Process Refund
        sagaInstance.addStep(SagaStep.create(
            "Process Refund",
            "PaymentService",
            "refundPayment",
            "chargePayment",
            orderId
        ));
        
        // Step 4: Send Cancellation Notification
        sagaInstance.addStep(SagaStep.create(
            "Send Cancellation Notification",
            "NotificationService",
            "sendOrderCancellation",
            "sendOrderConfirmation",
            orderId
        ));
    }
}
