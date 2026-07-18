package service;

import dao.CartDAO;
import dao.ProductDAO;
import dao.ProductModifierDAO;
import entity.Cart;
import entity.CartItem;
import entity.Product;
import entity.ProductVariant;
import entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CartService {
    private CartDAO cartDAO = new CartDAO();
    private ProductDAO productDAO = new ProductDAO();
    private ProductModifierDAO modifierDAO = new ProductModifierDAO();

    private Cart getOrCreateCart(User user) {
        Cart cart = cartDAO.findByUserId(user.getUserId());
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart.setCreatedAt(LocalDateTime.now());
            cart = cartDAO.create(cart);
        }
        return cart;
    }

    private Cart getCartByUser(User user) {
        return cartDAO.findByUserId(user.getUserId());
    }

    public Map<String, Object> getCart(User user) {
        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartDAO.getItems(cart.getCartId());

        List<Map<String, Object>> itemList = items.stream().map(ci -> {
            Map<String, Object> m = new HashMap<>();
            m.put("cartItemId", ci.getCartItemId());
            m.put("productId", ci.getProduct().getProductId());
            m.put("variantId", ci.getVariant() != null ? ci.getVariant().getVariantId() : null);
            m.put("productName", ci.getProduct().getName());
            m.put("variantName", ci.getVariant() != null ? ci.getVariant().getVariantName() : "");
            m.put("imageUrl", ci.getProduct().getImageUrl());
            m.put("quantity", ci.getQuantity());
            m.put("unitPrice", ci.getUnitPrice());
            m.put("quantityAvailable", ci.getVariant() != null ? ci.getVariant().getQuantityAvailable() : null);
            m.put("variantStatus", ci.getVariant() != null ? ci.getVariant().getStatus() : "UNAVAILABLE");
            m.put("productStatus", ci.getProduct().getStatus());
            List<Map<String, Object>> modifiers = new ArrayList<>();
            String selected = ci.getSelectedModifierOptionIds();
            if (selected != null && !selected.isBlank()) for (String id : selected.split(",")) try {
                var option = modifierDAO.option(Integer.parseInt(id));
                if (option != null) modifiers.add(Map.of("modifierOptionId", option.getModifierOptionId(), "groupName", option.getGroup().getName(), "name", option.getName(), "price", option.getPrice()));
            } catch (NumberFormatException ignored) {}
            m.put("modifiers", modifiers);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cart.getCartId());
        result.put("items", itemList);
        return result;
    }

    public boolean addItem(User user, int productId, int variantId, int quantity, List<Integer> modifierOptionIds) {
        if (quantity <= 0) return false;
        Product product = productDAO.findById(productId);
        if (product == null || !"AVAILABLE".equals(product.getStatus())) return false;
        ProductVariant variant = productDAO.findVariantById(variantId);
        if (variant == null || variant.getProduct() == null || variant.getProduct().getProductId() != productId) return false;
        if (!"AVAILABLE".equals(variant.getStatus())) return false;
        List<Integer> optionIds = modifierOptionIds != null ? modifierOptionIds.stream().distinct().collect(Collectors.toList()) : List.of();
        BigDecimal modifierPrice = BigDecimal.ZERO;
        Map<Integer, Integer> selectedByGroup = new HashMap<>();
        for (Integer optionId : optionIds) {
            var option = modifierDAO.option(optionId);
            if (option == null || !Boolean.TRUE.equals(option.getIsActive()) || option.getGroup() == null || option.getGroup().getProduct().getProductId() != productId || !Boolean.TRUE.equals(option.getGroup().getIsActive())) return false;
            selectedByGroup.merge(option.getGroup().getModifierGroupId(), 1, Integer::sum);
            modifierPrice = modifierPrice.add(option.getPrice() != null ? option.getPrice() : BigDecimal.ZERO);
        }
        for (var group : modifierDAO.groups(productId)) {
            if (Boolean.TRUE.equals(group.getIsActive())) {
                int selected = selectedByGroup.getOrDefault(group.getModifierGroupId(), 0);
                if (selected < group.getMinSelections() || selected > group.getMaxSelections()) return false;
            }
        }
        String modifierKey = optionIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));

        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartDAO.getItems(cart.getCartId());
        CartItem existing = items.stream()
                .filter(ci -> ci.getProduct().getProductId() == productId
                        && (ci.getVariant() != null && ci.getVariant().getVariantId() == variantId)
                        && modifierKey.equals(ci.getSelectedModifierOptionIds() == null ? "" : ci.getSelectedModifierOptionIds()))
                .findFirst().orElse(null);

        int newQty = quantity;
        if (existing != null) {
            newQty = existing.getQuantity() + quantity;
        }

        Integer stock = variant.getQuantityAvailable();
        if (stock != null && stock < newQty) return false;

        if (existing != null) {
            existing.setQuantity(newQty);
            cartDAO.updateItemQuantity(existing.getCartItemId(), newQty);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setVariant(variant);
            item.setQuantity(quantity);
            item.setUnitPrice((variant.getPrice() != null ? variant.getPrice() : product.getBasePrice()).add(modifierPrice));
            item.setSelectedModifierOptionIds(modifierKey);
            item.setCreatedAt(LocalDateTime.now());
            cartDAO.addItem(item);
        }
        return true;
    }

    public boolean updateItemQuantity(int cartItemId, int userId, int quantity) {
        Cart cart = getCartByUser(new User() {{ setUserId(userId); }});
        if (cart == null) return false;

        List<CartItem> items = cartDAO.getItems(cart.getCartId());
        CartItem item = items.stream()
                .filter(ci -> ci.getCartItemId() == cartItemId)
                .findFirst().orElse(null);
        if (item == null) return false;

        if (quantity <= 0) {
            cartDAO.removeItem(cartItemId);
            return true;
        }

        ProductVariant variant = item.getVariant();
        if (variant != null) {
            Integer stock = variant.getQuantityAvailable();
            if (stock != null && stock < quantity) return false;
        }

        cartDAO.updateItemQuantity(cartItemId, quantity);
        return true;
    }

    public boolean removeItem(int cartItemId, int userId) {
        Cart cart = getCartByUser(new User() {{ setUserId(userId); }});
        if (cart == null) return false;

        List<CartItem> items = cartDAO.getItems(cart.getCartId());
        boolean belongs = items.stream().anyMatch(ci -> ci.getCartItemId() == cartItemId);
        if (!belongs) return false;

        cartDAO.removeItem(cartItemId);
        return true;
    }
}
