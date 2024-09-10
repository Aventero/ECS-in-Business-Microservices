package thesis.ecommerce.productservice.component;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ProductDateComponent {
    private Instant creationDate;
    private Instant lastUpdated;
}
