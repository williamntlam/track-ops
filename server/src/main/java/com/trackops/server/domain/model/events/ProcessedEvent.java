@Entity
@Table(name = "processed_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ProcessedEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
    
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "consumer_group")
    private String consumerGroup;
    
    @Column(name = "partition")
    private Integer partition;
    
    @Column(name = "offset")
    private Long offset;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}