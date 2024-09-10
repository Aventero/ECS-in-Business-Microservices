package thesis.ecommerce.authservice.service;


import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class UserService {

    private final UserCredentialsRepository userRepository;

    public UserService(UserCredentialsRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> getUserByUsername(String username) {
        Optional<UserCredentialsModel> user = userRepository.findByUsername(username);
        return user.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}