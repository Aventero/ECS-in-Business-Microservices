package thesis.ecommerce.authservice.ecs.components;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;

import lombok.Data;

@Data
public class CompletableFutureComponent {
    private CompletableFuture<ResponseEntity<?>> future;

    public CompletableFutureComponent(CompletableFuture<ResponseEntity<?>> future) {
        this.future = future;
    }
}