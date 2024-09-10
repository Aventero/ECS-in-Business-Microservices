package thesis.ecommerce.orderservice.api.controller;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.orderservice.api.dto.order.PlaceOrderRequestDto;
import thesis.ecommerce.orderservice.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {


    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequestDto request, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();
        return orderService.placeOrder(request, username, token);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> viewOrder(@PathVariable UUID orderId) {
        return orderService.viewOrder(orderId);
    }


    @GetMapping("/history")
    public ResponseEntity<?> getOrderHistory(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        return orderService.getOrderHistory(username);
    }
}