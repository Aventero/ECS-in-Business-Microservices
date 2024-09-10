package thesis.ecommerce.orderservice.api.controller;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;
import thesis.ecommerce.orderservice.external.ProductServiceClient;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;
import thesis.ecommerce.orderservice.persistence.model.CartModel;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.CartRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AddToCartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @MockBean
    private ProductServiceClient productServiceClient;

    private UUID testProductId;
    private String authToken;

    @BeforeEach
    void setup() {
        testProductId = UUID.randomUUID();
        authToken = "test_auth_token";

        // Mock the product service response
        ProductResponseDto mockProduct = new ProductResponseDto(
            testProductId, "Test Product", "Description", BigDecimal.valueOf(10.00), 100,
            "Category", Instant.now(), Instant.now()
        );
        when(productServiceClient.getProduct(eq(testProductId), anyString()))
            .thenReturn(Mono.just(mockProduct));

        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void testAddToCart_Success() throws Exception {
        // Perform the add to cart request
        MvcResult result = mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"" + testProductId + "\",\"quantity\":2}"))
            .andExpect(status().isOk())
            .andReturn();

        // Get the response content
        String responseContent = result.getResponse().getContentAsString();

        // Assert on the response content
        assertThat(responseContent).contains("Item added to cart");

        // Verify database state
        Optional<CartModel> cart = cartRepository.findByUsername("testuser");
        assertThat(cart).isPresent();
        List<CartItemModel> cartItems = cartItemRepository.findByCartId(cart.get().getCartId());
        assertThat(cartItems).hasSize(1);
        assertThat(cartItems.getFirst().getProductId()).isEqualTo(testProductId);
        assertThat(cartItems.getFirst().getQuantity()).isEqualTo(2);

        // Verify interaction with ProductService
        verify(productServiceClient).getProduct(eq(testProductId), anyString());
    }

    @Test
    void testAddToCart_InvalidInput() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"invalid-uuid\",\"quantity\":-1}"))
            .andExpect(status().isBadRequest());
    }
}