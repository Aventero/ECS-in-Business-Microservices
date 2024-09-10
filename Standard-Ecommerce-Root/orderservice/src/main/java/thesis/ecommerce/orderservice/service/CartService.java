package thesis.ecommerce.orderservice.service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.orderservice.api.dto.cart.AddToCartRequestDto;
import thesis.ecommerce.orderservice.api.dto.cart.CartItemDto;
import thesis.ecommerce.orderservice.api.dto.cart.CartResponseDto;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;
import thesis.ecommerce.orderservice.api.dto.cart.RemoveFromCartRequestDto;
import thesis.ecommerce.orderservice.external.ProductServiceClient;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;
import thesis.ecommerce.orderservice.persistence.model.CartModel;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.CartRepository;

@Service
public class CartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CartService.class);

    private final ProductServiceClient productServiceClient;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(ProductServiceClient productServiceClient,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository) {
        this.productServiceClient = productServiceClient;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public ResponseEntity<?> addToCart(AddToCartRequestDto request, String username, String token) {
        try {
            ProductResponseDto product = retrieveProduct(request.getProductId(), token);
            CartModel cart = getOrCreateCart(username);
            addItemToCart(cart, product, request.getQuantity());

            LOGGER.info("Added product item {} to cart for user {}", product.getId(), username);
            return ResponseEntity.ok("Item added to cart");
        } catch (Exception e) {
            LOGGER.error("Error adding item to cart: ", e);
            return ResponseEntity.badRequest().body("Error adding item to cart: " + e.getMessage());
        }
    }

    public ResponseEntity<?> removeFromCart(RemoveFromCartRequestDto request, String username) {
        LOGGER.info("Processing remove item from cart request for user: {}", username);

        Optional<CartModel> cartOpt = cartRepository.findByUsername(username);
        if (cartOpt.isEmpty()) {
            LOGGER.warn("Cart not found for user: {}", username);
            return ResponseEntity.badRequest().body("Cart not found");
        }

        CartModel cart = cartOpt.get();
        UUID itemId = UUID.fromString(request.cartItemId());

        Optional<CartItemModel> cartItemOpt = cartItemRepository.findById(itemId.toString());
        if (cartItemOpt.isEmpty() || !cartItemOpt.get().getCartId().equals(cart.getCartId())) {
            LOGGER.warn("Item {} not found in cart for user {}", itemId, username);
            return ResponseEntity.badRequest().body("Item not found in cart");
        }

        cartItemRepository.deleteById(itemId.toString());
        LOGGER.info("Removed item {} from cart for user {}", itemId, username);

        return ResponseEntity.ok("Item removed from cart");
    }

    @Transactional
    public ResponseEntity<String> emptyCart(String username) {
        LOGGER.info("Attempting to clear cart for user: {}", username);

        Optional<CartModel> userCart = cartRepository.findByUsername(username);

        if (userCart.isEmpty()) {
            LOGGER.warn("No cart found for user: {}", username);
            return ResponseEntity.badRequest().body("Cart not found");
        }

        cartItemRepository.deleteAllByCartId(userCart.get().getCartId());
        return ResponseEntity.ok("Clear cart successful for user: " + username);
    }

    public ResponseEntity<?> viewCart(String username, String token) {
        LOGGER.info("Processing view cart request for user: {}", username);

        try {
            CartModel cart = getOrCreateCart(username);
            List<CartItemModel> cartItems = cartItemRepository.findByCartId(cart.getCartId());
            List<ProductResponseDto> products = retrieveProducts(cartItems, token);

            CartResponseDto responseDto = createCartResponseDto(cart.getCartId(), cartItems, products);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            LOGGER.error("Error creating cart response: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private List<ProductResponseDto> retrieveProducts(List<CartItemModel> cartItems, String token) {
        List<UUID> productIds = cartItems.stream()
            .map(CartItemModel::getProductId)
            .toList();

        return productIds.parallelStream()
            .map(productId -> retrieveProduct(productId.toString(), token))
            .collect(Collectors.toList());
    }

    private ProductResponseDto retrieveProduct(String productId, String token) {
        LOGGER.info("Retrieving product information for product ID: {}", productId);
        return productServiceClient.getProduct(UUID.fromString(productId), token)
            .block();
    }

    private CartModel getOrCreateCart(String username) {
        return cartRepository.findByUsername(username)
            .orElseGet(() -> {
                CartModel newCart = new CartModel();
                newCart.setCartId(UUID.randomUUID());
                newCart.setUsername(username);
                Instant now = Instant.now();
                newCart.setCreatedAt(now);
                newCart.setUpdatedAt(now);
                return cartRepository.save(newCart);
            });
    }

    private CartResponseDto createCartResponseDto(UUID cartId, List<CartItemModel> cartItems,
        List<ProductResponseDto> products) {
        List<CartItemDto> items = cartItems.stream()
            .map(item -> mapToCartItemDto(item, products))
            .collect(Collectors.toList());

        return new CartResponseDto(cartId, items, calculateTotal(items));
    }

    private CartItemDto mapToCartItemDto(CartItemModel cartItem, List<ProductResponseDto> products) {
        ProductResponseDto productInfo = products.stream()
            .filter(product -> product.getId().equals(cartItem.getProductId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Product not found for ID: " + cartItem.getProductId()));

        return new CartItemDto(
            UUID.fromString(cartItem.getCartItemId()),
            cartItem.getProductId(),
            cartItem.getQuantity(),
            cartItem.getPrice(),
            productInfo
        );
    }

    private BigDecimal calculateTotal(List<CartItemDto> items) {
        return items.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private CartItemModel addItemToCart(CartModel cart, ProductResponseDto product, int quantity) {
        CartItemModel cartItem = new CartItemModel();
        cartItem.setCartItemId(UUID.randomUUID().toString());
        cartItem.setCartId(cart.getCartId());
        cartItem.setProductId(UUID.fromString(product.getId().toString()));
        cartItem.setPrice(product.getPrice());
        cartItem.setQuantity(quantity);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return cartItemRepository.save(cartItem);
    }
}