package thesis.ecommerce.productservice.component;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchCriteriaComponent {
    private String keyword;
    private String category;
}