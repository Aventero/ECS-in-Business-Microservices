package thesis.ecommerce.orderservice.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;
import thesis.ecommerce.orderservice.external.ProductServiceClient;
import thesis.ecommerce.orderservice.persistence.model.OrderItemModel;
import thesis.ecommerce.orderservice.persistence.model.OrderModel;
import thesis.ecommerce.orderservice.persistence.repository.OrderItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.OrderRepository;

@Service
public class OrderProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductServiceClient productServiceClient;

    public OrderProcessingService(OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ProductServiceClient productServiceClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productServiceClient = productServiceClient;
    }

    @Transactional
    public void processOrder(UUID orderId) {
        LOGGER.info("PROCESSING ORDER: {}", orderId);

        OrderModel order = orderRepository.findById(orderId.toString())
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (updateInventory(order)) {
            completeOrder(order);
        } else {
            cancelOrder(order);
        }
    }

    private boolean updateInventory(OrderModel order) {
        LOGGER.info("PROCESSING INVENTORY FOR ORDER: {}", order.getId());
        String token = (String) SecurityContextHolder.getContext().getAuthentication()
            .getCredentials();

        Map<UUID, Integer> productQuantities = getOrderItems(order.getId()).stream()
            .collect(Collectors.groupingBy(
                OrderItemModel::getProductId,
                Collectors.summingInt(OrderItemModel::getQuantity)
            ));

        boolean allSuccessful = productQuantities.entrySet().stream()
            .allMatch(entry -> reduceInventoryByQuantity(entry.getKey(), entry.getValue(), token));

        if (allSuccessful) {
            LOGGER.info("Inventory successfully updated for order {}", order.getId());
        } else {
            LOGGER.warn("Inventory update failed for order {}. Flagging for cancellation.", order.getId());
        }

        return allSuccessful;
    }

    private boolean reduceInventoryByQuantity(UUID productId, int totalQuantity, String token) {
        try {
            return Boolean.TRUE.equals(
                productServiceClient.reduceInventory(productId, totalQuantity, token).block());
        } catch (Exception e) {
            LOGGER.error("Error during inventory update for product {}: {}", productId, e.getMessage());
            return false;
        }
    }

    private List<OrderItemModel> getOrderItems(String orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    private void completeOrder(OrderModel order) {
        LOGGER.info("COMPLETING ORDER: {}", order.getId());

        order.setStatus(OrderStatus.COMPLETED);
        order.setOrderDateTime(Instant.now());
        orderRepository.save(order);

        LOGGER.info("Order {} has been successfully completed", order.getId());
    }

    private void cancelOrder(OrderModel order) {
        LOGGER.warn("CANCELLING ORDER: {} - Reason: {}", order.getId(), "Inventory update failed");

        order.setStatus(OrderStatus.CANCELLED);
        order.setOrderDateTime(Instant.now());
        orderRepository.save(order);

        LOGGER.info("Order {} has been cancelled", order.getId());
    }
}