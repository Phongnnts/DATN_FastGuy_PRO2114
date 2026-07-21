package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "CartItem")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private int cartItemId;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "selected_modifier_option_ids")
    @Transient
    private String selectedModifierOptionIds;

    @Column(name = "modifiers_json")
    private String modifiersJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public CartItem() {}

    public int getCartItemId() { return cartItemId; }
    public void setCartItemId(int cartItemId) { this.cartItemId = cartItemId; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getSelectedModifierOptionIds() { return selectedModifierOptionIds; }
    public void setSelectedModifierOptionIds(String selectedModifierOptionIds) { this.selectedModifierOptionIds = selectedModifierOptionIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
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

    public void clearModifiers() {
        this.modifiersJson = "[]";
    }

    public static class ModifierItem {
        public int modifierOptionId;
        public int groupId;
        public String groupName;
        public String name;
        public BigDecimal price;

        public ModifierItem() {}
        public ModifierItem(int modifierOptionId, int groupId, String groupName, String name, BigDecimal price) {
            this.modifierOptionId = modifierOptionId;
            this.groupId = groupId;
            this.groupName = groupName;
            this.name = name;
            this.price = price;
        }
    }
}
