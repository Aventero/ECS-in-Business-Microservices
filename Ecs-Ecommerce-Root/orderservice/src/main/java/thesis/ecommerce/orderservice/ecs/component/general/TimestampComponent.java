package thesis.ecommerce.orderservice.ecs.component.general;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimestampComponent {

    private Instant createdAt;
    private Instant updatedAt;
}