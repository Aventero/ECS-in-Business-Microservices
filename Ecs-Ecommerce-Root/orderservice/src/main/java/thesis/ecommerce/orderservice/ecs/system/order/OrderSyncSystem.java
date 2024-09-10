package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.ecs.component.order.CustomerInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderStatusComponent;
import thesis.ecommerce.orderservice.ecs.component.order.PaymentInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.ShippingInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.Flags.Order;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrder;
import thesis.ecommerce.orderservice.persistence.model.OrderModel;
import thesis.ecommerce.orderservice.persistence.repository.OrderRepository;

@Service
public class OrderSyncSystem implements  Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSyncSystem.class);
    private final ECSWorld world;
    private final OrderRepository orderRepository;

    public OrderSyncSystem(ECSWorld world, OrderRepository orderRepository) {
        this.world = world;
        this.orderRepository = orderRepository;
        world.registerSystem(this);
    }

    @Override
    public void run() {
        world.getDominion()
            .findEntitiesWith(
                Order.class,
                SyncOrder.class,
                OrderIdComponent.class)
            .forEach(result -> syncOrder(result.entity()));
    }

    private void syncOrder(Entity order) {
        LOGGER.info("PROCESSING SYNCING ORDER");

        order.removeType(SyncOrder.class);
        OrderModel orderModel = mapToOrderModel(order);
        LOGGER.info("Syncing order: {}", order.get(OrderIdComponent.class).orderId());
        orderRepository.save(orderModel);
    }

    private OrderModel mapToOrderModel(Entity order) {
        OrderModel orderModel = new OrderModel();
        orderModel.setId(order.get(OrderIdComponent.class).orderId().toString());
        orderModel.setUsername(order.get(AuthenticationComponent.class).username());
        orderModel.setOrderDateTime(order.get(OrderStatusComponent.class).timestamp());
        orderModel.setStatus(order.get(OrderStatusComponent.class).status());
        orderModel.setTotalAmount(order.get(PriceComponent.class).getPrice());
        orderModel.setCustomerName(order.get(CustomerInfoComponent.class).name());
        orderModel.setEmail(order.get(CustomerInfoComponent.class).email());
        orderModel.setBillingAddress(order.get(CustomerInfoComponent.class).address());
        orderModel.setPaymentMethod(order.get(PaymentInfoComponent.class).paymentMethod());
        orderModel.setPaymentDetails(order.get(PaymentInfoComponent.class).transactionId());
        orderModel.setShippingAddress(order.get(ShippingInfoComponent.class).address());
        orderModel.setShippingMethod(order.get(ShippingInfoComponent.class).shippingMethod());
        return orderModel;
    }
}