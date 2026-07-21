package service;

import dao.ProductDAO;
import entity.Product;
import entity.ProductVariant;
import entity.User;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

public class UserFavoriteService {
    private ProductDAO productDAO = new ProductDAO();

    public List<Map<String, Object>> getByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            if (user == null) return List.of();
            List<FavEntry> entries = parseFavorites(user.getFavoriteIdsJson());
            if (entries.isEmpty()) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (FavEntry entry : entries) {
                Product product = productDAO.findById(entry.productId);
                if (product != null && "AVAILABLE".equals(product.getStatus())) {
                    result.add(toProductMap(product));
                }
            }
            return result;
        } finally {
            em.close();
        }
    }

    public boolean isFavorite(int userId, int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            if (user == null) return false;
            return parseFavorites(user.getFavoriteIdsJson()).stream().anyMatch(e -> e.productId == productId);
        } finally {
            em.close();
        }
    }

    public Map<String, Object> toggle(int userId, int productId) {
        Product product = productDAO.findById(productId);
        if (product == null || !"AVAILABLE".equals(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId);
            if (user == null) { em.getTransaction().rollback(); throw new IllegalArgumentException("User not found"); }
            List<FavEntry> entries = parseFavorites(user.getFavoriteIdsJson());
            boolean exists = entries.stream().anyMatch(e -> e.productId == productId);
            boolean favorite;
            if (exists) {
                entries.removeIf(e -> e.productId == productId);
                favorite = false;
            } else {
                entries.add(new FavEntry(productId, LocalDateTime.now().toString()));
                favorite = true;
            }
            user.setFavoriteIdsJson(serializeFavorites(entries));
            em.getTransaction().commit();
            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("favorite", favorite);
            return data;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private List<FavEntry> parseFavorites(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) return new ArrayList<>();
        try {
            return utils.JsonUtil.getMapper().readValue(json, new TypeReference<List<FavEntry>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeFavorites(List<FavEntry> entries) {
        try {
            return utils.JsonUtil.getMapper().writeValueAsString(entries);
        } catch (Exception e) {
            return "[]";
        }
    }

    static class FavEntry {
        public int productId;
        public String createdAt;
        public FavEntry() {}
        public FavEntry(int productId, String createdAt) {
            this.productId = productId;
            this.createdAt = createdAt;
        }
    }

    private Map<String, Object> toProductMap(Product product) {
        ProductVariant defaultVariant = productDAO.findDefaultVariantByProductId(product.getProductId());
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getProductId());
        data.put("name", product.getName());
        data.put("description", product.getDescription() != null ? product.getDescription() : "");
        data.put("basePrice", product.getBasePrice());
        data.put("price", defaultVariant != null ? defaultVariant.getPrice() : product.getBasePrice());
        data.put("defaultVariant", defaultVariant != null ? toVariantMap(defaultVariant) : null);
        data.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "");
        data.put("categoryId", product.getCategory() != null ? product.getCategory().getCategoryId() : null);
        data.put("categoryName", product.getCategory() != null ? product.getCategory().getName() : "");
        data.put("discountPrice", null);
        data.put("inStock", "AVAILABLE".equals(product.getStatus()));
        data.put("featured", false);
        return data;
    }

    private Map<String, Object> toVariantMap(ProductVariant variant) {
        Map<String, Object> data = new HashMap<>();
        data.put("variantId", variant.getVariantId());
        data.put("variantName", variant.getVariantName());
        data.put("price", variant.getPrice());
        data.put("originalPrice", variant.getOriginalPrice());
        data.put("sku", variant.getSku());
        data.put("quantityAvailable", variant.getQuantityAvailable());
        data.put("isDefault", variant.getIsDefault() != null ? variant.getIsDefault() : false);
        data.put("status", variant.getStatus());
        return data;
    }
}
