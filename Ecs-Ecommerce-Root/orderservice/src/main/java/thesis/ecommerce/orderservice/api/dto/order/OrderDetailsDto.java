package thesis.ecommerce.orderservice.api.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailsDto(
    String orderId,
    OrderStatus status,
    Instant orderDate,
    BigDecimal totalPrice,
    List<OrderItemDto> items,
    String customerName,
    String customerEmail,
    String billingAddress,
    String shippingAddress,
    String shippingMethod
) {}