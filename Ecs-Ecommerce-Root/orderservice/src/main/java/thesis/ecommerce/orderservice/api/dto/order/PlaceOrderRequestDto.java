package thesis.ecommerce.orderservice.api.dto.order;

@SuppressWarnings("unused")
public record PlaceOrderRequestDto(
    String customerName,
    String email,
    String address,
    String paymentMethod,
    String paymentDetails,
    String shippingAddress,
    String shippingMethod
) {
}