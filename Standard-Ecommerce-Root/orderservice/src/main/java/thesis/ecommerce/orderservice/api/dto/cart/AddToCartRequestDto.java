package thesis.ecommerce.orderservice.api.dto.cart;

import lombok.Data;

@Data
public class AddToCartRequestDto {

    private String productId;
    private int quantity;
}