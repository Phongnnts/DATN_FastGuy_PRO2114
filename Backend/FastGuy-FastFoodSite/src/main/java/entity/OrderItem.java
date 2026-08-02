package entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "OrderItem")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private int orderItemId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "modifiers_json")
    private String modifiersJson;

    public OrderItem() {}

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int orderItemId) { this.orderItemId = orderItemId; }
    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public List<ModifierItem> getModifiers() {
        if (modifiersJson == null || modifiersJson.isEmpty()) return new ArrayList<>();
        try {
            return utils.JsonUtil.getMapper().readValue(modifiersJson, new com.fasterxml.jackson.core.type.TypeReference<List<ModifierItem>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setModifiers(List<ModifierItem> modifiers) {
        try {
            this.modifiersJson = modifiers == null || modifiers.isEmpty() ? "[]" : utils.JsonUtil.getMapper().writeValueAsString(modifiers);
        } catch (Exception e) {
            this.modifiersJson = "[]";
        }
    }

    public static class ModifierItem {
        public int modifierOptionId;
        public String groupName;
        public String optionName;
        public BigDecimal price;

        public ModifierItem() {}
        public ModifierItem(int modifierOptionId, String groupName, String optionName, BigDecimal price) {
            this.modifierOptionId = modifierOptionId;
            this.groupName = groupName;
            this.optionName = optionName;
            this.price = price;
        }
    }
}
