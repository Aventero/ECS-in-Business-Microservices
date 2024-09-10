package thesis.ecommerce.orderservice.api.controller;

import dev.dominion.ecs.api.Entity;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.api.dto.order.PlaceOrderRequestDto;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.CompletableFutureComponent;
import thesis.ecommerce.orderservice.ecs.component.order.CustomerInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdsComponent;
import thesis.ecommerce.orderservice.ecs.component.order.PaymentInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.ShippingInfoComponent;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ECSWorld ecsWorld;

    public OrderController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @PostMapping("/place")
    public CompletableFuture<ResponseEntity<?>> placeOrder(@RequestBody PlaceOrderRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();

        Entity entity = ecsWorld.createEntity();
        entity.add(new AuthenticationComponent(username, token));
        entity.add(new CustomerInfoComponent(request.customerName(), request.email(), request.address()));
        entity.add(new PaymentInfoComponent(request.paymentMethod(), request.paymentDetails()));
        entity.add(new ShippingInfoComponent(request.shippingAddress(), request.shippingMethod()));
        entity.add(new Flags.PlaceOrder());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(future));

        return future;
    }

    @GetMapping("/{orderId}")
    public CompletableFuture<ResponseEntity<?>> viewOrder(@PathVariable UUID orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String token = (String) authentication.getCredentials();
        String username = (String) authentication.getPrincipal();

        Entity entity = ecsWorld.createEntity();
        entity.add(new AuthenticationComponent(username, token));
        entity.add(new OrderIdsComponent(List.of(orderId)));
        entity.add(new Flags.FetchOrder());
        entity.add(new Flags.ViewOrder());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(future));

        return future;
    }

    @GetMapping("/history")
    public CompletableFuture<ResponseEntity<?>> getOrderHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String token = (String) authentication.getCredentials();
        String username = (String) authentication.getPrincipal();

        Entity sendOrderFetchingSignal = ecsWorld.createEntity();
        sendOrderFetchingSignal.add(new AuthenticationComponent(username, token));
        sendOrderFetchingSignal.add(new Flags.InitiateOrderFetching());
        sendOrderFetchingSignal.add(new Flags.ViewOrderHistory());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        sendOrderFetchingSignal.add(new CompletableFutureComponent(future));

        return future;
    }
}