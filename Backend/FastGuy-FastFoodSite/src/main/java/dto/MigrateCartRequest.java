package dto;

import java.util.List;

public class MigrateCartRequest {
    private List<AddToCartRequest> items;

    public List<AddToCartRequest> getItems() { return items; }
    public void setItems(List<AddToCartRequest> items) { this.items = items; }
}
