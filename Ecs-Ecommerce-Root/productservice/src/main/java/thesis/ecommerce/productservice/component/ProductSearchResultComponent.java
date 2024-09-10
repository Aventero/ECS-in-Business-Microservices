package thesis.ecommerce.productservice.component;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductSearchResultComponent {

    private String id;
    private String name;
    private String description;
    private int stock;
    private BigDecimal price;
    private String category;
}