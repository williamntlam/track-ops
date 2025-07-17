import com.trackops.server.domain.model.orders.Order;
import com.trackops.server.domain.model.enums.OrderStatus;
import java.util.Optional;
import java.util.List;

public interface OrderRepository {

    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);

}