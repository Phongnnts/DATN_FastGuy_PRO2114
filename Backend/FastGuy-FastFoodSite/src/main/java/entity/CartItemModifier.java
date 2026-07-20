package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "CartItemModifier")
public class CartItemModifier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_modifier_id")
    private int cartItemModifierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_option_id", nullable = false)
    private ProductModifierOption modifierOption;

    public CartItemModifier() {}
    public CartItemModifier(CartItem cartItem, ProductModifierOption modifierOption) {
        this.cartItem = cartItem;
        this.modifierOption = modifierOption;
    }

    public int getCartItemModifierId() { return cartItemModifierId; }
    public CartItem getCartItem() { return cartItem; }
    public void setCartItem(CartItem cartItem) { this.cartItem = cartItem; }
    public ProductModifierOption getModifierOption() { return modifierOption; }
    public void setModifierOption(ProductModifierOption modifierOption) { this.modifierOption = modifierOption; }
}
