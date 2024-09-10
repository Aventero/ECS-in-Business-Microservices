package thesis.ecommerce.orderservice.ecs.component.general;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriceComponent {

    private BigDecimal price;
}