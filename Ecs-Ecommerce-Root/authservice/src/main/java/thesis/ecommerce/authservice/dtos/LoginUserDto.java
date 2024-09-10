package thesis.ecommerce.authservice.dtos;

import lombok.Data;

@Data
public class LoginUserDto {
    private String username;
    private String password;
}
