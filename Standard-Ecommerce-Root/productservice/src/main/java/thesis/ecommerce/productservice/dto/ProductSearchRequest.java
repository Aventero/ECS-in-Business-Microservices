package thesis.ecommerce.productservice.dto;

import lombok.Data;

@Data
public class ProductSearchRequest {
    private String keyword;
    private String category;
}