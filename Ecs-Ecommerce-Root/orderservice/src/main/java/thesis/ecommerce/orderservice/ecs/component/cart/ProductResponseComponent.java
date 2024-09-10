package thesis.ecommerce.orderservice.ecs.component.cart;

import java.util.List;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;

public record ProductResponseComponent (List<ProductResponseDto> productResponses) {
}