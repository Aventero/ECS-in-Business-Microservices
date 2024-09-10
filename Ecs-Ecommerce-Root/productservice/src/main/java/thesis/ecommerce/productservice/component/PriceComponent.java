package thesis.ecommerce.productservice.component;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriceComponent {

    private BigDecimal price;
}