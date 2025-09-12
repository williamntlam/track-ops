package com.trackops.server.adapters.input.web.controllers;

import com.trackops.server.application.services.saga.SagaManagementService;
import com.trackops.server.domain.model.saga.SagaInstance;
import com.trackops.server.domain.model.saga.SagaStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sagas")
public class SagaController {
    
    private final SagaManagementService sagaManagementService;

    public SagaController(SagaManagementService sagaManagementService) {
        this.sagaManagementService = sagaManagementService;
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaInstance> getSagaStatus(@PathVariable String sagaId) {
        try {
            SagaInstance saga = sagaManagementService.getSagaStatus(sagaId);
            if (saga != null) {
                return ResponseEntity.ok(saga);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<SagaInstance>> getSagasForOrder(@PathVariable String orderId) {
        try {
            List<SagaInstance> sagas = sagaManagementService.getSagasForOrder(orderId);
            return ResponseEntity.ok(sagas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SagaInstance>> getSagasByStatus(@PathVariable String status) {
        try {
            SagaStatus sagaStatus = SagaStatus.valueOf(status.toUpperCase());
            List<SagaInstance> sagas = sagaManagementService.getSagasByStatus(sagaStatus);
            return ResponseEntity.ok(sagas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{sagaId}/retry")
    public ResponseEntity<String> retrySaga(@PathVariable String sagaId) {
        try {
            boolean success = sagaManagementService.retrySaga(sagaId);
            if (success) {
                return ResponseEntity.ok("SAGA retry initiated successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to retry SAGA");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error retrying SAGA: " + e.getMessage());
        }
    }

    @PostMapping("/{sagaId}/compensate")
    public ResponseEntity<String> compensateSaga(@PathVariable String sagaId) {
        try {
            boolean success = sagaManagementService.compensateSaga(sagaId);
            if (success) {
                return ResponseEntity.ok("SAGA compensation initiated successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to compensate SAGA");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error compensating SAGA: " + e.getMessage());
        }
    }
}
