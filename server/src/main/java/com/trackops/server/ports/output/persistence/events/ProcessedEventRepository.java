import com.trackops.server.domain.model.events.ProcessedEvent;
import java.util.Optional;

public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);
    Optional<ProcessedEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
    void deleteByEventId(String eventId);
}