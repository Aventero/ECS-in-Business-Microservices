package thesis.ecommerce.orderservice.api.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(
    UUID itemId,
    UUID productId,
    int quantity,
    BigDecimal price,
    ProductResponseDto product
) {

}