package thesis.ecommerce.productservice.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;

@Data
@AllArgsConstructor
public class CompletableFutureComponent {
    private CompletableFuture<ResponseEntity<?>> future;

}