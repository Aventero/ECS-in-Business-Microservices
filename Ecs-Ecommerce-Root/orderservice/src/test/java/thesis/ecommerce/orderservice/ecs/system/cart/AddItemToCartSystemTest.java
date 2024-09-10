package thesis.ecommerce.orderservice.ecs.system.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import dev.dominion.ecs.api.Entity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemsComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AddItemToCartSystemTest {

    static Logger logger = LoggerFactory.getLogger(AddItemToCartSystem.class);
    private static ECSWorld ecsWorld;
    private static AddItemToCartSystem addItemToCartSystem;
    @Mock
    private static CartItemRepository cartItemRepository;

    @BeforeAll
    static public void setUp() {
        logger.info("SETUP");
        ecsWorld = new ECSWorld();
        cartItemRepository = mock(CartItemRepository.class);
        addItemToCartSystem = new AddItemToCartSystem(ecsWorld, cartItemRepository);
    }

    @Test
    public void testAddItemToCart_Success() throws InterruptedException {
        Entity processEntity = createProcessEntity();
        Entity cartEntity = createCartEntity();

        addItemToCartSystem.run();

        HttpStatusCode ok = processEntity.get(FutureResponseComponent.class).getResult().getStatusCode();
        assertEquals(HttpStatus.OK, ok);

        // cleanup
        ecsWorld.getDominion().deleteEntity(processEntity);
        ecsWorld.getDominion().deleteEntity(cartEntity);
    }

    @Test
    public void testAddItemToCart_NotFound() {
        Entity processEntity = createProcessEntity();
        addItemToCartSystem.run();
        HttpStatusCode notFound = processEntity.get(FutureResponseComponent.class).getResult().getStatusCode();
        assertEquals(HttpStatus.NOT_FOUND, notFound);

        // cleanup
        ecsWorld.getDominion().deleteEntity(processEntity);
    }

    private Entity createProcessEntity() {
        ProductResponseDto mockProduct = new ProductResponseDto(
            UUID.randomUUID(), "Test Product", "Description", BigDecimal.valueOf(10.00), 100,
            "Category", Instant.now(), Instant.now());

        // Mock entity and components
        Entity processEntity = ecsWorld.createEntity();
        processEntity.add(new Flags.AddItemToCart());
        processEntity.add(new Flags.CartCreated());
        processEntity.add(new Flags.ProductRetrieved());
        processEntity.add(new ProductResponseComponent(List.of(mockProduct)));
        processEntity.add(new AuthenticationComponent("username", "token"));
        processEntity.add(mock(QuantityComponent.class));
        return processEntity;
    }

    private Entity createCartEntity() {
        Entity cartEntity = ecsWorld.createEntity();
        cartEntity.add(new AuthenticationComponent("username", "token"));
        cartEntity.add(new Flags.Cart());
        UUID cartId = UUID.randomUUID();
        cartEntity.add(new CartIdComponent(cartId));
        cartEntity.add(new CartItemsComponent(new HashSet<>()));
        cartEntity.add(new QuantityComponent(1));
        return cartEntity;
    }

}