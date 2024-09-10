package thesis.ecommerce.productservice.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class FutureResponseComponent {
    private ResponseEntity<?> result;
}
