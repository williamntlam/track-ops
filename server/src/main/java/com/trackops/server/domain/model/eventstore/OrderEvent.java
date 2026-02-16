package com.trackops.server.domain.model.eventstore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only event store record for order event sourcing and auditability.
 * Persisted via Spring Data JPA (no raw JDBC).
 */
@Entity
@Table(
    name = "order_events",
    uniqueConstraints = @UniqueConstraint(columnNames = { "order_id", "sequence_number" })
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
