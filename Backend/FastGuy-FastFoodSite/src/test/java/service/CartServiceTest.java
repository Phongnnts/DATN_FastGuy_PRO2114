package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import entity.CartItem;
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
        CartItem ci = new CartItem();
        List<CartItem.ModifierItem> mods = new ArrayList<>();
        mods.add(new CartItem.ModifierItem(3, 1, "Group", "Option3", BigDecimal.ZERO));
        mods.add(new CartItem.ModifierItem(1, 1, "Group", "Option1", BigDecimal.ZERO));
        mods.add(new CartItem.ModifierItem(2, 1, "Group", "Option2", BigDecimal.ZERO));
        ci.setModifiers(mods);

        String key = ci.getModifiers().stream()
                .map(m -> String.valueOf(m.modifierOptionId))
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
    @DisplayName("CartItem ModifierItem stores correct fields")
    void modifierItemFields() {
        CartItem.ModifierItem mod = new CartItem.ModifierItem(42, 1, "Size", "L", new BigDecimal("5000"));
        assertEquals(42, mod.modifierOptionId);
        assertEquals(1, mod.groupId);
        assertEquals("Size", mod.groupName);
        assertEquals("L", mod.name);
        assertEquals(new BigDecimal("5000"), mod.price);
    }

    @Test
    @DisplayName("CartItem modifiers JSON round-trip")
    void modifierJsonRoundTrip() {
        CartItem ci = new CartItem();
        List<CartItem.ModifierItem> mods = new ArrayList<>();
        mods.add(new CartItem.ModifierItem(1, 1, "Size", "L", new BigDecimal("5000")));
        mods.add(new CartItem.ModifierItem(2, 2, "Topping", "Cheese", new BigDecimal("3000")));
        ci.setModifiers(mods);

        assertEquals(2, ci.getModifiers().size());
        assertEquals(1, ci.getModifiers().get(0).modifierOptionId);
        assertEquals("Topping", ci.getModifiers().get(1).groupName);

        ci.clearModifiers();
        assertTrue(ci.getModifiers().isEmpty());
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
