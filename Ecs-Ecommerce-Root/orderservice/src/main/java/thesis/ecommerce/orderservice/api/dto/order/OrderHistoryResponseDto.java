package thesis.ecommerce.orderservice.api.dto.order;

import java.util.List;

public record OrderHistoryResponseDto(List<OrderDetailsDto> orders) {

}