package com.trackops.server.application.services.saga;

import com.trackops.server.domain.model.saga.SagaInstance;
import com.trackops.server.domain.model.saga.SagaStepEntity;
import com.trackops.server.ports.output.persistence.orders.OrderRepository;
import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import com.trackops.server.ports.output.events.orders.OrderEventProducer;
import com.trackops.server.domain.events.orders.OrderStatusUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderSagaStepExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(OrderSagaStepExecutor.class);
    
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderSagaStepExecutor(OrderRepository orderRepository, OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    public boolean executeStep(SagaStepEntity step, SagaInstance sagaInstance) {
        try {
            log.info("Executing step: {} for order: {}", step.getStepName(), sagaInstance.getAggregateId());
            
            UUID orderId = UUID.fromString(sagaInstance.getAggregateId());
            
            switch (step.getStepName()) {
                case "Validate Order":
                    return validateOrder(orderId);
                    
                case "Reserve Inventory":
                    return reserveInventory(orderId);
                    
                case "Process Payment":
                    return processPayment(orderId);
                    
                case "Update Order Status":
                    return updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                    
                case "Send Notification":
                    return sendNotification(orderId, "Order confirmed successfully");
                    
                case "Cancel Order":
                    return updateOrderStatus(orderId, OrderStatus.CANCELLED);
                    
                case "Release Inventory":
                    return releaseInventory(orderId);
                    
                case "Process Refund":
                    return processRefund(orderId);
                    
                case "Send Cancellation Notification":
                    return sendNotification(orderId, "Order cancelled successfully");
                    
                default:
                    log.warn("Unknown step: {}", step.getStepName());
                    return false;
            }
        } catch (Exception e) {
            log.error("Error executing step {}: {}", step.getStepName(), e.getMessage(), e);
            return false;
        }
    }

    public boolean executeCompensation(SagaStepEntity step, SagaInstance sagaInstance) {
        try {
            log.info("Executing compensation for step: {} for order: {}", step.getStepName(), sagaInstance.getAggregateId());
            
            UUID orderId = UUID.fromString(sagaInstance.getAggregateId());
            
            switch (step.getStepName()) {
                case "Validate Order":
                    return cancelOrder(orderId);
                    
                case "Reserve Inventory":
                    return releaseInventory(orderId);
                    
                case "Process Payment":
                    return processRefund(orderId);
                    
                case "Update Order Status":
                    return revertOrderStatus(orderId);
                    
                case "Send Notification":
                    return sendNotification(orderId, "Order processing failed");
                    
                case "Cancel Order":
                    return restoreOrder(orderId);
                    
                case "Release Inventory":
                    return reserveInventory(orderId);
                    
                case "Process Refund":
                    return processPayment(orderId);
                    
                case "Send Cancellation Notification":
                    return sendNotification(orderId, "Order cancellation failed");
                    
                default:
                    log.warn("Unknown compensation step: {}", step.getStepName());
                    return false;
            }
        } catch (Exception e) {
            log.error("Error executing compensation for step {}: {}", step.getStepName(), e.getMessage(), e);
            return false;
        }
    }

    // Step implementations
    private boolean validateOrder(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            // Simulate validation logic
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new RuntimeException("Order is not in PENDING status");
            }
            
            log.info("Order {} validated successfully", orderId);
            return true;
        } catch (Exception e) {
            log.error("Order validation failed for {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean reserveInventory(UUID orderId) {
        try {
            // Simulate inventory reservation
            log.info("Inventory reserved for order: {}", orderId);
            Thread.sleep(100); // Simulate network call
            return true;
        } catch (Exception e) {
            log.error("Inventory reservation failed for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean processPayment(UUID orderId) {
        try {
            // Simulate payment processing
            log.info("Payment processed for order: {}", orderId);
            Thread.sleep(200); // Simulate network call
            return true;
        } catch (Exception e) {
            log.error("Payment processing failed for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            OrderStatus previousStatus = order.getStatus();
            
            // Update order status based on the new status
            switch (newStatus) {
                case CONFIRMED:
                    order.confirm();
                    break;
                case CANCELLED:
                    order.cancel();
                    break;
                default:
                    throw new RuntimeException("Unsupported status transition to: " + newStatus);
            }
            
            Order updatedOrder = orderRepository.save(order);
            
            // Publish event
            OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                orderId, 
                previousStatus, 
                newStatus, 
                updatedOrder.getVersion()
            );
            orderEventProducer.publishOrderStatusUpdated(event);
            
            log.info("Order {} status updated to {}", orderId, newStatus);
            return true;
        } catch (Exception e) {
            log.error("Failed to update order {} status: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean sendNotification(UUID orderId, String message) {
        try {
            // Simulate notification sending
            log.info("Notification sent for order {}: {}", orderId, message);
            Thread.sleep(50); // Simulate network call
            return true;
        } catch (Exception e) {
            log.error("Notification failed for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean cancelOrder(UUID orderId) {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    private boolean releaseInventory(UUID orderId) {
        try {
            // Simulate inventory release
            log.info("Inventory released for order: {}", orderId);
            Thread.sleep(100); // Simulate network call
            return true;
        } catch (Exception e) {
            log.error("Inventory release failed for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean processRefund(UUID orderId) {
        try {
            // Simulate refund processing
            log.info("Refund processed for order: {}", orderId);
            Thread.sleep(200); // Simulate network call
            return true;
        } catch (Exception e) {
            log.error("Refund processing failed for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean revertOrderStatus(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            // Revert to PENDING status
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(OrderStatus.PENDING);
            
            Order updatedOrder = orderRepository.save(order);
            
            // Publish event
            OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                orderId, 
                previousStatus, 
                OrderStatus.PENDING, 
                updatedOrder.getVersion()
            );
            orderEventProducer.publishOrderStatusUpdated(event);
            
            log.info("Order {} status reverted to PENDING", orderId);
            return true;
        } catch (Exception e) {
            log.error("Failed to revert order {} status: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean restoreOrder(UUID orderId) {
        return updateOrderStatus(orderId, OrderStatus.PENDING);
    }
}
