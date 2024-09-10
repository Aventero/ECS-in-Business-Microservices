package thesis.ecommerce.authservice.dtos;

import lombok.Data;

@Data
public class RegisterUserDto {
    private String username;
    private String password;
}
