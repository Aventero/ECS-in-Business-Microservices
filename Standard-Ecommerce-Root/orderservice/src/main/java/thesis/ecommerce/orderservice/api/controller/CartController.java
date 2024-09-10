package thesis.ecommerce.orderservice.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.orderservice.api.dto.cart.AddToCartRequestDto;
import thesis.ecommerce.orderservice.api.dto.cart.RemoveFromCartRequestDto;
import thesis.ecommerce.orderservice.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddToCartRequestDto request, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();
        return cartService.addToCart(request, username, token);
    }


    @PostMapping("/remove")
    public ResponseEntity<?> removeFromCart(@RequestBody RemoveFromCartRequestDto request,
        Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        return cartService.removeFromCart(request, username);
    }

    @PostMapping("/empty")
    public ResponseEntity<String> clearCart(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        return cartService.emptyCart(username);
    }

    @GetMapping
    public ResponseEntity<?> viewCart(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();
        return cartService.viewCart(username, token);
    }
}