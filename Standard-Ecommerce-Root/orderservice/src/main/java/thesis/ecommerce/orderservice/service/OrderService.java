package thesis.ecommerce.orderservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thesis.ecommerce.orderservice.api.dto.order.OrderDetailsDto;
import thesis.ecommerce.orderservice.api.dto.order.OrderHistoryResponseDto;
import thesis.ecommerce.orderservice.api.dto.order.OrderItemDto;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;
import thesis.ecommerce.orderservice.api.dto.order.PlaceOrderRequestDto;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;
import thesis.ecommerce.orderservice.persistence.model.CartModel;
import thesis.ecommerce.orderservice.persistence.model.OrderItemModel;
import thesis.ecommerce.orderservice.persistence.model.OrderModel;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.CartRepository;
import thesis.ecommerce.orderservice.persistence.repository.OrderItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderProcessingService orderProcessingService;

    public OrderService(CartRepository cartRepository, CartItemRepository cartItemRepository,
        OrderRepository orderRepository, OrderItemRepository orderItemRepository,
        OrderProcessingService orderProcessingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderProcessingService = orderProcessingService;
    }

    @Transactional
    public ResponseEntity<?> placeOrder(PlaceOrderRequestDto request, String username, String token) {
        LOGGER.info("PROCESSING PLACE ORDER");

        CartModel cart = findCartForUser(username);
        if (cart == null) {
            LOGGER.warn("Cart not found for user: {}", username);
            return ResponseEntity.badRequest().body("Cart not found");
        }

        List<CartItemModel> cartItems = cartItemRepository.findByCartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            LOGGER.warn("Cart is empty for user: {}", username);
            return ResponseEntity.badRequest().body("Cart is empty");
        }

        OrderModel order = createOrderFromCart(cart, request, username);
        List<OrderItemModel> orderItems = createOrderItemsFromCart(cartItems, order);
        order.setTotalAmount(calculateTotal(orderItems));

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        emptyCart(cart);

        LOGGER.info("Order created with ID: {}", order.getId());
        // Trigger order processing
        orderProcessingService.processOrder(UUID.fromString(order.getId()));

        return ResponseEntity.ok("Order placed successfully. Order ID: " + order.getId());
    }

    public ResponseEntity<?> viewOrder(UUID orderId) {
        LOGGER.info("PROCESSING VIEW ORDER REQUEST for orderId: {}", orderId);

        try {
            OrderModel order = orderRepository.findById(orderId.toString())
                .orElseThrow(() -> new RuntimeException("Order not found"));

            List<OrderItemModel> orderItems = orderItemRepository.findByOrderId(orderId.toString());

            OrderDetailsDto responseDto = createOrderDetailsDto(order, orderItems);

            LOGGER.info("Finish view order request for order ID: {}", responseDto.orderId());
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            LOGGER.error("Error viewing order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public ResponseEntity<?> getOrderHistory(String username) {
        LOGGER.info("STARTING ORDER HISTORY REQUEST for user: {}", username);

        List<String> orderIds = orderRepository.findOrderIdsByUsername(username);

        if (orderIds.isEmpty()) {
            LOGGER.info("No orders found for user: {}", username);
            return ResponseEntity.noContent().build();
        }

        List<OrderDetailsDto> orderHistory = createOrderDetails(orderIds);
        OrderHistoryResponseDto response = new OrderHistoryResponseDto(orderHistory);
        return ResponseEntity.ok(response);
    }

    private List<OrderDetailsDto> createOrderDetails(List<String> orderIds) {
        return orderIds.stream()
            .map(this::getSingleOrderDetails).toList();
    }

    private OrderDetailsDto getSingleOrderDetails(String orderId) {
        OrderModel order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        List<OrderItemModel> orderItems = orderItemRepository.findByOrderId(orderId);

        return createOrderDetailsDto(order, orderItems);
    }

    private CartModel findCartForUser(String username) {
        return cartRepository.findByUsername(username).orElse(null);
    }

    private OrderDetailsDto createOrderDetailsDto(OrderModel order, List<OrderItemModel> orderItems) {
        return new OrderDetailsDto(
            order.getId(),
            order.getStatus(),
            order.getOrderDateTime(),
            order.getTotalAmount(),
            mapOrderItems(orderItems),
            order.getCustomerName(),
            order.getEmail(),
            order.getBillingAddress(),
            order.getShippingAddress(),
            order.getShippingMethod()
        );
    }

    private List<OrderItemDto> mapOrderItems(List<OrderItemModel> orderItems) {
        return orderItems.stream()
            .map(this::mapToOrderItemDto)
            .collect(Collectors.toList());
    }

    private OrderItemDto mapToOrderItemDto(OrderItemModel orderItem) {
        return new OrderItemDto(
            orderItem.getProductId(),
            orderItem.getQuantity(),
            orderItem.getUnitPrice()
        );
    }

    private OrderModel createOrderFromCart(CartModel cart,
        PlaceOrderRequestDto request, String username) {
        OrderModel order = new OrderModel();
        order.setCartId(cart.getCartId());
        order.setUsername(username);
        order.setId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.CREATED);
        order.setOrderDateTime(Instant.now());
        order.setCustomerName(request.customerName());
        order.setEmail(request.email());
        order.setBillingAddress(request.address());
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentDetails(request.paymentDetails());
        order.setShippingAddress(request.shippingAddress());
        order.setShippingMethod(request.shippingMethod());
        return order;
    }

    private List<OrderItemModel> createOrderItemsFromCart(List<CartItemModel> cartItems, OrderModel order) {
        List<OrderItemModel> orderItems = new ArrayList<>();
        for (CartItemModel cartItem : cartItems) {
            OrderItemModel orderItem = new OrderItemModel();
            orderItem.setId(UUID.randomUUID().toString());
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    private BigDecimal calculateTotal(List<OrderItemModel> orderItems) {
        return orderItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void emptyCart(CartModel cart) {
        cartItemRepository.deleteAllByCartId(cart.getCartId());
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }
}