package thesis.ecommerce.orderservice.ecs.component;

public final class Flags {

    private Flags() {
    }

    // CART
    public record SyncCartItem() {}
    public record CreateCart() {}
    public record CartCreated() {}
    public record Cart() {}
    public record CartItem() {}
    public record AddItemToCart() {}
    public record EmptyCart() {}
    public record RemoveItemFromCart() {}
    public record CartItemAdded() {}
    public record ViewCart() {}
    public record RetrieveProduct() {}
    public record RetrieveProductsForCart() {}
    public record ProductRetrieved() {}
    public record CartProductsRetrieved() {}

    // ORDER
    public record Order() {}
    public record FetchedOrder() {}
    public record OrderItem() {}
    public record SyncOrder() {}
    public record SyncOrderItem() {}
    public record PlaceOrder() {}
    public record ViewOrder() {}
    public record ViewOrderHistory() {}
    public record OrderCompleted() {}
    public record UpdateInventory() {}
    public record InventoryUpdateSuccess() {}
    public record RequiresCancellation() {}
    public record DeleteOrderEntity() {}
    public record DeleteOrderItemEntity() {}

    // Signal
    public record InitiateOrderFetching() {}
    public record FetchOrder() {}
    public record OrdersReady() {}

}