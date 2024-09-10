package thesis.ecommerce.orderservice.ecs.component.general;

import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class CompletableFutureComponent {

    private CompletableFuture<ResponseEntity<?>> future;

}