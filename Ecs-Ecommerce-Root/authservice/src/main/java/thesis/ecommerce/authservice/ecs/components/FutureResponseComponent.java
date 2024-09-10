package thesis.ecommerce.authservice.ecs.components;

import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FutureResponseComponent {
    private ResponseEntity<?> result;
}
