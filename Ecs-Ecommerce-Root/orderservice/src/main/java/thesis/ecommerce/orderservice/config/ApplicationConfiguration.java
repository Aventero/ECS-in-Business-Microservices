package thesis.ecommerce.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import thesis.ecommerce.ECSWorld;

@Configuration
public class ApplicationConfiguration {

    @Bean
    ECSWorld ecsWorld() {
        return new ECSWorld();
    }
}
