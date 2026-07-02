package service;

import dao.CartDAO;
import dao.ProductDAO;
import entity.Cart;
import entity.CartItem;
import entity.Product;
import entity.ProductVariant;
import entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CartService {
    private CartDAO cartDAO = new CartDAO();
    private ProductDAO productDAO = new ProductDAO();

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
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cart.getCartId());
        result.put("items", itemList);
        return result;
    }

    public boolean addItem(User user, int productId, int variantId, int quantity) {
        if (quantity <= 0) return false;
        Product product = productDAO.findById(productId);
        if (product == null || !"AVAILABLE".equals(product.getStatus())) return false;
        ProductVariant variant = productDAO.findVariantById(variantId);
        if (variant == null || variant.getProduct() == null || variant.getProduct().getProductId() != productId) return false;
        if (!"AVAILABLE".equals(variant.getStatus())) return false;

        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartDAO.getItems(cart.getCartId());
        CartItem existing = items.stream()
                .filter(ci -> ci.getProduct().getProductId() == productId
                        && (ci.getVariant() != null && ci.getVariant().getVariantId() == variantId))
                .findFirst().orElse(null);

        int newQty = quantity;
        if (existing != null) {
            newQty = existing.getQuantity() + quantity;
        }

        if (variant.getQuantityAvailable() != null && variant.getQuantityAvailable() < newQty) return false;

        if (existing != null) {
            existing.setQuantity(newQty);
            existing.setUnitPrice(variant.getPrice());
            cartDAO.updateItemQuantity(existing.getCartItemId(), newQty);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setVariant(variant);
            item.setQuantity(quantity);
            item.setUnitPrice(variant.getPrice());
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
        if (variant != null && variant.getQuantityAvailable() != null && variant.getQuantityAvailable() < quantity) {
            return false;
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
