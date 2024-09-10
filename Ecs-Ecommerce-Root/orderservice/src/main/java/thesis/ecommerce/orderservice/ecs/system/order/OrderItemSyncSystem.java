package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrderItem;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderItem;
import thesis.ecommerce.orderservice.persistence.model.OrderItemModel;
import thesis.ecommerce.orderservice.persistence.repository.OrderItemRepository;

@Service
public class OrderItemSyncSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderItemSyncSystem.class);
    private final ECSWorld world;
    private final OrderItemRepository orderItemRepository;

    public OrderItemSyncSystem(ECSWorld world, OrderItemRepository orderRepository) {
        this.world = world;
        this.orderItemRepository = orderRepository;
        world.registerSystem(this);
    }

    @Override
    public void run() {
        world.getDominion()
            .findEntitiesWith(
                SyncOrderItem.class,
                OrderItem.class,
                OrderItemIdComponent.class)
            .forEach(result -> syncOrderItem(result.entity()));
    }

    private void syncOrderItem(Entity orderItem) {
        LOGGER.info("PROCESSING SYNCING ORDER ITEM");
        orderItem.removeType(SyncOrderItem.class);

        OrderItemModel orderModel = mapToOrderItemModel(orderItem);
        LOGGER.info("Syncing Order Item: {}", orderItem.get(OrderItemIdComponent.class).orderItemId());
        orderItemRepository.save(orderModel);
    }

    private OrderItemModel mapToOrderItemModel(Entity orderItem) {
        OrderItemModel orderItemModel = new OrderItemModel();
        orderItemModel.setId(orderItem.get(OrderItemIdComponent.class).orderItemId().toString());
        orderItemModel.setOrderId(orderItem.get(OrderIdComponent.class).orderId().toString());
        orderItemModel.setProductId(orderItem.get(ProductReferenceComponent.class).productItemId());
        orderItemModel.setQuantity(orderItem.get(QuantityComponent.class).getQuantity());
        orderItemModel.setUnitPrice(orderItem.get(PriceComponent.class).getPrice());
        return orderItemModel;
    }
}