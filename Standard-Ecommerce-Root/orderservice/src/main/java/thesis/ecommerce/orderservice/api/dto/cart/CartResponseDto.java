package thesis.ecommerce.orderservice.api.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponseDto(UUID cartId, List<CartItemDto> items, BigDecimal total) {

}