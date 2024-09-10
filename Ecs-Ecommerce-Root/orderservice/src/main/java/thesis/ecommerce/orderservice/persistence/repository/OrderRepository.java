package thesis.ecommerce.orderservice.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thesis.ecommerce.orderservice.persistence.model.OrderModel;

@Repository
public interface OrderRepository extends JpaRepository<OrderModel, String> {
    @Query("SELECT o.id FROM OrderModel o WHERE o.username = :username")
    List<String > findOrderIdsByUsername(@Param("username") String username);
}