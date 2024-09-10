package thesis.ecommerce.authservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Configuration
@EnableAsync
public class AppConfiguration {
    private final UserCredentialsRepository userCredentialsRepository;
    private static final Logger logger = LoggerFactory.getLogger(AppConfiguration.class);

    public AppConfiguration(UserCredentialsRepository userRepository) {
        this.userCredentialsRepository = userRepository;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            logger.info("Attempting to load user details for username: {}", username);
            return userCredentialsRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.warn("User not found: {}", username);
                        return new UsernameNotFoundException("User not found");
                    });
        };
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    ECSWorld ecsWorld() {
        return new ECSWorld();
    }
}
