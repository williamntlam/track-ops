import com.trackops.server.domain.model.events.ProcessedEvent;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);
    Optional<ProcessedEvent> findByEventId(UUID eventId);
    Optional<ProcessedEvent> findByOrderId(UUID orderId);  // Add this line
    boolean existsByEventId(UUID eventId);
    void deleteByEventId(UUID eventId);
}