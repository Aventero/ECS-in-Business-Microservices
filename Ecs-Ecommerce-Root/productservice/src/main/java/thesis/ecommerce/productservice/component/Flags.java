package thesis.ecommerce.productservice.component;

public final class Flags {

    private Flags() {
    }

    public record GetProduct() {}
    public record CreateProduct() {}
    public record UpdateProduct() {}
    public record DeleteProduct() {}
    public record SearchProducts() {}
    public record ReduceInventory() {}
    public record RequiresDeletion() {}

    // Database lock flags
    public record RequestDatabaseLock() {}
    public record ReleaseDatabaseLock() {}
    public record DatabaseLockAcquired() {}
    public record DatabaseProcessingFinished() {}
}