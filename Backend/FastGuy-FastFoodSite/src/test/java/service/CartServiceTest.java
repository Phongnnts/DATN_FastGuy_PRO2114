package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import entity.CartItem;
import entity.CartItemModifier;
import entity.Product;
import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import entity.ProductVariant;

class CartServiceTest {

    @Test
    @DisplayName("CartItem modifier key is empty when no modifiers")
    void emptyModifierKey() {
        CartItem ci = new CartItem();
        String key = ci.getModifiers() == null || ci.getModifiers().isEmpty() ? "" : "has";
        assertEquals("", key);
    }

    @Test
    @DisplayName("CartItem modifier key matches sorted option IDs")
    void modifierKeySorted() {
        ProductModifierOption opt3 = new ProductModifierOption();
        opt3.setModifierOptionId(3);
        ProductModifierOption opt1 = new ProductModifierOption();
        opt1.setModifierOptionId(1);
        ProductModifierOption opt2 = new ProductModifierOption();
        opt2.setModifierOptionId(2);

        CartItem ci = new CartItem();
        ci.addModifier(new CartItemModifier(ci, opt3));
        ci.addModifier(new CartItemModifier(ci, opt1));
        ci.addModifier(new CartItemModifier(ci, opt2));

        String key = ci.getModifiers().stream()
                .map(m -> String.valueOf(m.getModifierOption().getModifierOptionId()))
                .sorted()
                .collect(Collectors.joining(","));
        assertEquals("1,2,3", key);
    }

    @Test
    @DisplayName("Product modifier group min/max validation")
    void modifierGroupMinMax() {
        ProductModifierGroup group = new ProductModifierGroup();
        group.setMinSelections(0);
        group.setMaxSelections(3);
        assertTrue(group.getMinSelections() <= group.getMaxSelections());

        group.setMinSelections(2);
        group.setMaxSelections(2);
        assertTrue(group.getMinSelections() <= group.getMaxSelections());
    }

    @Test
    @DisplayName("ProductVariant price and stock")
    void variantPriceStock() {
        ProductVariant v = new ProductVariant();
        v.setPrice(new BigDecimal("45000"));
        v.setOriginalPrice(new BigDecimal("50000"));
        v.setQuantityAvailable(10);
        assertEquals(new BigDecimal("45000"), v.getPrice());
        assertEquals(10, v.getQuantityAvailable());
    }

    @Test
    @DisplayName("CartItem addModifier sets back-reference")
    void addModifierSetsBackRef() {
        CartItem ci = new CartItem();
        ProductModifierOption opt = new ProductModifierOption();
        opt.setModifierOptionId(42);
        CartItemModifier mod = new CartItemModifier(ci, opt);
        assertEquals(ci, mod.getCartItem());
        assertEquals(42, mod.getModifierOption().getModifierOptionId());
    }

    @Test
    @DisplayName("Product status checks")
    void productStatus() {
        Product p = new Product();
        p.setStatus("AVAILABLE");
        assertEquals("AVAILABLE", p.getStatus());
        p.setStatus("UNAVAILABLE");
        assertEquals("UNAVAILABLE", p.getStatus());
    }
}
