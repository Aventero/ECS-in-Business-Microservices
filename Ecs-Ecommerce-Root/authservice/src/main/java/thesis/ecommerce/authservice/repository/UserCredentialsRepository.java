package thesis.ecommerce.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import thesis.ecommerce.authservice.model.UserCredentialsModel;

public interface UserCredentialsRepository extends JpaRepository<UserCredentialsModel, Long> {
    boolean existsByUsername(String username);

    Optional<UserCredentialsModel> findByUsername(String username);
}