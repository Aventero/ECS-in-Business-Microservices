package thesis.ecommerce.productservice.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductSearchResponse {

    private String id;
    private String name;
    private String description;
    private int stock;
    private BigDecimal price;
    private String category;
}