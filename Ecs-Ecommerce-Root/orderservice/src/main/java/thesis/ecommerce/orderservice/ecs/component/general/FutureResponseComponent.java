package thesis.ecommerce.orderservice.ecs.component.general;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class FutureResponseComponent {

    private ResponseEntity<?> result;
}