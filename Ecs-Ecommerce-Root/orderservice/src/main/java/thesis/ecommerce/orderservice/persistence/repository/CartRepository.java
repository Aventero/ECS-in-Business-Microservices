package thesis.ecommerce.orderservice.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thesis.ecommerce.orderservice.persistence.model.CartModel;

@Repository
public interface CartRepository extends JpaRepository<CartModel, String> {

    Optional<CartModel> findByUsername(String username);
}