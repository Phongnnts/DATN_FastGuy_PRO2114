package dto;

import java.util.List;

public class CreateOrderRequest {
    private Long addressId;
    private String paymentMethod;
    private String internalNote;
    private List<CreateOrderItem> items;

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public List<CreateOrderItem> getItems() { return items; }
    public void setItems(List<CreateOrderItem> items) { this.items = items; }

    public static class CreateOrderItem {
        private Long productId;
        private Integer quantity;
        private String optionData;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getOptionData() { return optionData; }
        public void setOptionData(String optionData) { this.optionData = optionData; }
    }
}
