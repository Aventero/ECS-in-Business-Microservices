package thesis.ecommerce.orderservice.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thesis.ecommerce.orderservice.persistence.model.OrderItemModel;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemModel, String> {

    List<OrderItemModel> findByOrderId(String id);
}