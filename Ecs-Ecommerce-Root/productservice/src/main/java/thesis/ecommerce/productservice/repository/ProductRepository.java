package thesis.ecommerce.productservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import thesis.ecommerce.productservice.model.ProductModel;

public interface ProductRepository extends JpaRepository<ProductModel, Long> {

    boolean existsById(UUID id);

    void deleteById(UUID id);

    Optional<ProductModel> findById(UUID id);

    List<ProductModel> findByNameContainingIgnoreCase(String name);

    List<ProductModel> findByCategoryIgnoreCase(String category);

    List<ProductModel> findByCategoryAndNameContainingIgnoreCase(String category, String name);
}