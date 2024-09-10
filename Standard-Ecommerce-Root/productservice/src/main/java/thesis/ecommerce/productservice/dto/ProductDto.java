package thesis.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductDto {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String category;
    private Instant creationDate;
    private Instant lastUpdated;
}