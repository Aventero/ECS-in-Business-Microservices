package thesis.ecommerce.orderservice.ecs.component.order;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderCancellationComponent {
    List<String> reasons;
}