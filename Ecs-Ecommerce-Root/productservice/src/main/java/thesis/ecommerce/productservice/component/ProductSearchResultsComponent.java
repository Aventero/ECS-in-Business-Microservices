package thesis.ecommerce.productservice.component;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductSearchResultsComponent {
    private List<ProductSearchResultComponent> results;
}