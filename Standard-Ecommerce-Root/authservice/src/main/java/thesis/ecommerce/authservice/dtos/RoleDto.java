package thesis.ecommerce.authservice.dtos;

import lombok.Data;
import thesis.ecommerce.authservice.model.UserRole;

@Data
public class RoleDto {
    private String username;
    private UserRole role;
}