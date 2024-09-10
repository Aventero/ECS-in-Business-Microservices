package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.ecs.component.order.CustomerInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderDetailsComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdsComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderStatusComponent;
import thesis.ecommerce.orderservice.ecs.component.order.PaymentInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.ShippingInfoComponent;
import thesis.ecommerce.orderservice.api.dto.order.OrderDetailsDto;
import thesis.ecommerce.orderservice.api.dto.order.OrderItemDto;
import thesis.ecommerce.orderservice.ecs.component.Flags.FetchOrder;
import thesis.ecommerce.orderservice.ecs.component.Flags.FetchedOrder;
import thesis.ecommerce.orderservice.ecs.component.Flags.Order;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderItem;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrdersReady;
import thesis.ecommerce.orderservice.persistence.model.OrderModel;
import thesis.ecommerce.orderservice.persistence.repository.OrderItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.OrderRepository;

@Service
public class FetchOrdersSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchOrdersSystem.class);
    private final ECSWorld ecsWorld;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public FetchOrdersSystem(ECSWorld ecsWorld,
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository) {
        this.ecsWorld = ecsWorld;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        processOrderFetchRequests();
    }

    private void processOrderFetchRequests() {
        ecsWorld.getDominion()
            .findEntitiesWith(FetchOrder.class, OrderIdsComponent.class)
            .forEach(request -> {
                try {
                    fetchOrders(request.entity());
                } catch (Exception e) {
                    LOGGER.error("Error fetching orders", e);
                    request.entity().add(new FutureResponseComponent(ResponseEntity.badRequest().body("Error fetching orders")));
                }
            });
    }

    private void fetchOrders(Entity request) {
        LOGGER.info("PROCESSING FETCH ORDERS REQUEST");

        List<UUID> orderIds = request.get(OrderIdsComponent.class).orderIds();
        orderIds.forEach(orderId -> fetchOrderFromMemory(orderId).ifPresentOrElse(
            this::createFetchedOrderEntity,
            () -> fetchOrderFromDatabase(orderId).ifPresent(
                this::createFetchedOrderEntity)
        ));
        request.removeType(FetchOrder.class);
        request.add(new OrdersReady());
    }

    private Optional<OrderDetailsDto> fetchOrderFromMemory(UUID orderId) {
        return ecsWorld.getDominion().findEntitiesWith(Order.class, OrderIdComponent.class)
            .stream()
            .filter(result -> result.comp2().orderId().equals(orderId))
            .findFirst()
            .map(result -> createOrderDetailsDto(result.entity(), getOrderItems(orderId)));
    }

    private Optional<OrderDetailsDto> fetchOrderFromDatabase(UUID orderId) {
        return orderRepository.findById(orderId.toString())
            .map(this::createOrderDetailsDto);
    }

    private void createFetchedOrderEntity(OrderDetailsDto orderDetailsDto) {
        ecsWorld.createEntity(
            new FetchedOrder(),
            new OrderDetailsComponent(orderDetailsDto)
        );
    }

    private OrderDetailsDto createOrderDetailsDto(Entity orderEntity, List<Entity> orderItemEntities) {
        OrderIdComponent orderIdComp = orderEntity.get(OrderIdComponent.class);
        OrderStatusComponent statusComp = orderEntity.get(OrderStatusComponent.class);
        CustomerInfoComponent customerInfoComp = orderEntity.get(CustomerInfoComponent.class);
        ShippingInfoComponent shippingInfoComp = orderEntity.get(ShippingInfoComponent.class);
        PaymentInfoComponent paymentInfoComp = orderEntity.get(PaymentInfoComponent.class);

        List<OrderItemDto> items = orderItemEntities.stream()
            .map(this::createOrderItemDto)
            .toList();

        BigDecimal totalAmount = calculateTotalAmount(items);

        return new OrderDetailsDto(
            orderIdComp.orderId().toString(),
            statusComp.status(),
            statusComp.timestamp(),
            totalAmount,
            items,
            customerInfoComp.name(),
            customerInfoComp.email(),
            paymentInfoComp.paymentMethod(),
            shippingInfoComp.address(),
            shippingInfoComp.shippingMethod()
        );
    }

    private OrderItemDto createOrderItemDto(Entity orderItemEntity) {
        return new OrderItemDto(
            orderItemEntity.get(ProductReferenceComponent.class).productItemId(),
            orderItemEntity.get(QuantityComponent.class).getQuantity(),
            orderItemEntity.get(PriceComponent.class).getPrice()
        );
    }

    private List<Entity> getOrderItems(UUID orderId) {
        return ecsWorld.getDominion()
            .findEntitiesWith(OrderItem.class, OrderIdComponent.class)
            .stream()
            .filter(result -> result.comp2().orderId().equals(orderId))
            .map(With2::entity)
            .toList();
    }

    private OrderDetailsDto createOrderDetailsDto(OrderModel order) {
        List<OrderItemDto> items = orderItemRepository.findByOrderId(order.getId()).stream()
            .map(item -> new OrderItemDto(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .toList();

        return new OrderDetailsDto(
            order.getId(),
            order.getStatus(),
            order.getOrderDateTime(),
            order.getTotalAmount(),
            items,
            order.getCustomerName(),
            order.getEmail(),
            order.getBillingAddress(),
            order.getShippingAddress(),
            order.getShippingMethod()
        );
    }

    private BigDecimal calculateTotalAmount(List<OrderItemDto> items) {
        return items.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}