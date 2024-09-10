package thesis.ecommerce.orderservice.ecs.component.order;

import java.time.Instant;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;

public record OrderStatusComponent(OrderStatus status, Instant timestamp) {
}