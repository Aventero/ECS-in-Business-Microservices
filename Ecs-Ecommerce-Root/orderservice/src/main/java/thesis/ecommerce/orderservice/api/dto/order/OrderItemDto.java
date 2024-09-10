package thesis.ecommerce.orderservice.api.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
    UUID productId,
    int quantity,
    BigDecimal price
) {

}