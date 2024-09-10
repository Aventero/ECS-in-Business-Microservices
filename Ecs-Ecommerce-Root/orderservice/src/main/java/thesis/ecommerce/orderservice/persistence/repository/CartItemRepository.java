package thesis.ecommerce.orderservice.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemModel, String> {

    List<CartItemModel> findByCartId(UUID cartId);
}