package thesis.ecommerce.orderservice.persistence.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "cart_items")
public class CartItemModel {
    @Id
    private String cartItemId;

    private UUID cartId;

    private UUID productId;

    private BigDecimal price;

    private int quantity;
}