package thesis.ecommerce.productservice.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResponse {

    private String id;
    private String name;
    private String description;
    private int stock;
    private BigDecimal price;
    private String category;
}