package thesis.ecommerce.orderservice.ecs.component.cart;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItemsComponent {

    private Set<UUID> cartItemIds;
}