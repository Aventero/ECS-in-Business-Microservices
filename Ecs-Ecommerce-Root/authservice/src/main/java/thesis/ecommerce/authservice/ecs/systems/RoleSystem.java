package thesis.ecommerce.authservice.ecs.systems;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.FutureResponseComponent;
import thesis.ecommerce.authservice.ecs.components.RoleComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.model.UserRole;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class RoleSystem implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RoleSystem.class);
    private final ECSWorld world;
    private final UserCredentialsRepository userRepository;

    RoleSystem(ECSWorld world, UserCredentialsRepository userRepository) {
        this.world = world;
        this.userRepository = userRepository;
    }

    @Override
    public void run() {
        logger.debug("RoleSystem run() called");
        processAddRole();
        processRemoveRole();
        processGetRoles();
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing RoleSystem");
        world.registerSystem(this);
    }

    private void processAddRole() {
        logger.debug("Processing add role requests");
        world.getDominion().findEntitiesWith(Flags.AddRole.class)
                .forEach(result -> {
                    Entity entity = result.entity();
                    try {
                        String username = entity.get(UsernameComponent.class).getUsername();
                        UserRole role = entity.get(RoleComponent.class).getRole();
                        logger.info("Adding role {} to user {}", role, username);

                        UserCredentialsModel user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

                        user.getRoles().add(role.name());
                        userRepository.save(user);
                        logger.info("Role {} successfully added to user {}", role, username);

                        entity.add(new FutureResponseComponent(ResponseEntity.ok("Role added successfully")));
                    } catch (Exception e) {
                        logger.error("Error adding role: ", e);
                        entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(e.getMessage())));
                    } finally {
                        removeRoleComponents(entity);
                    }
                });
    }

    private void processRemoveRole() {
        logger.debug("Processing remove role requests");
        world.getDominion().findEntitiesWith(Flags.RemoveRole.class)
                .forEach(result -> {
                    Entity entity = result.entity();
                    try {
                        String username = entity.get(UsernameComponent.class).getUsername();
                        UserRole role = entity.get(RoleComponent.class).getRole();
                        logger.info("Removing role {} from user {}", role, username);

                        UserCredentialsModel user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
                        if (user.getRoles().remove(role.name())) {
                            userRepository.save(user);
                            logger.info("Role {} successfully removed from user {}", role, username);
                            entity.add(new FutureResponseComponent(ResponseEntity.ok("Role removed successfully")));
                        } else {
                            logger.warn("User {} did not have the role {}", username, role);
                            entity.add(new FutureResponseComponent(
                                    ResponseEntity.badRequest().body("User did not have the specified role")));
                        }
                    } catch (Exception e) {
                        logger.error("Error removing role: ", e);
                        entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(e.getMessage())));
                    } finally {
                        removeRoleComponents(entity);
                    }
                });
    }

    private void processGetRoles() {
        logger.debug("Processing get roles requests");
        world.getDominion()
                .findEntitiesWith(Flags.GetRoles.class)
                .forEach(result -> {
                    Entity entity = result.entity();
                    try {
                        String username = entity.get(UsernameComponent.class).getUsername();
                        logger.info("Getting roles for user {}", username);

                        UserCredentialsModel user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
                        Set<String> roles = user.getRoles();
                        logger.info("Retrieved roles for user {}: {}", username, roles);

                        entity.add(new FutureResponseComponent(ResponseEntity.ok(roles)));
                    } catch (Exception e) {
                        logger.error("Error getting roles: ", e);
                        entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(e.getMessage())));
                    } finally {
                        removeRoleComponents(entity);
                    }
                });
    }

    private void removeRoleComponents(Entity entity) {
        logger.debug("Removing role components from entity");
        entity.removeType(Flags.AddRole.class);
        entity.removeType(Flags.RemoveRole.class);
        entity.removeType(Flags.GetRoles.class);
        entity.removeType(UsernameComponent.class);
        entity.removeType(RoleComponent.class);
    }
}