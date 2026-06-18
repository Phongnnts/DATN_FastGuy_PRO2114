package service;

import dao.CartDAO;
import dao.ProductDAO;
import entity.Cart;
import entity.CartItem;
import entity.Product;
import entity.User;

import java.math.BigDecimal;
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

    public Map<String, Object> getCart(User user) {
        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartDAO.getItems(cart.getCartId());

        List<Map<String, Object>> itemList = items.stream().map(ci -> {
            Map<String, Object> m = new HashMap<>();
            m.put("cartItemId", ci.getCartItemId());
            m.put("productId", ci.getProduct().getProductId());
            m.put("productName", ci.getProduct().getName());
            m.put("imageUrl", ci.getProduct().getImageUrl());
            m.put("quantity", ci.getQuantity());
            m.put("unitPrice", ci.getUnitPrice());
            m.put("optionData", ci.getOptionData() != null ? ci.getOptionData() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cart.getCartId());
        result.put("items", itemList);
        return result;
    }

    public boolean addItem(User user, int productId, int quantity, BigDecimal unitPrice, String optionData) {
        Cart cart = getOrCreateCart(user);
        CartItem item = new CartItem();
        item.setCart(cart);
        Product product = productDAO.findById(productId);
        if (product == null) return false;
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setOptionData(optionData);
        cartDAO.addItem(item);
        return true;
    }

    public boolean updateItemQuantity(int cartItemId, int quantity) {
        if (quantity <= 0) {
            cartDAO.removeItem(cartItemId);
        } else {
            cartDAO.updateItemQuantity(cartItemId, quantity);
        }
        return true;
    }

    public boolean removeItem(int cartItemId) {
        cartDAO.removeItem(cartItemId);
        return true;
    }
}
