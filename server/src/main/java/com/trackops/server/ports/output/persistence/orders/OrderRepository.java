import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);
    Optional<Order> findById(UUID id);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);

}