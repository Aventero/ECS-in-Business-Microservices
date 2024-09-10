package thesis.ecommerce.authservice.ecs.components;

import lombok.AllArgsConstructor;
import lombok.Data;
import thesis.ecommerce.authservice.model.UserRole;

@Data
@AllArgsConstructor
public class RoleComponent {
    private UserRole role;
}