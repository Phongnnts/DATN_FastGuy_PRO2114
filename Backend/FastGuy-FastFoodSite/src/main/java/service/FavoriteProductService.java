package service;

import dao.FavoriteProductDAO;
import dao.ProductDAO;
import entity.FavoriteProduct;
import entity.Product;
import entity.ProductVariant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FavoriteProductService {
    private FavoriteProductDAO favoriteDAO = new FavoriteProductDAO();
    private ProductDAO productDAO = new ProductDAO();

    public List<Map<String, Object>> getByUserId(int userId) {
        return favoriteDAO.findByUserId(userId).stream()
                .map(f -> toProductMap(f.getProduct()))
                .collect(Collectors.toList());
    }

    public boolean isFavorite(int userId, int productId) {
        return favoriteDAO.exists(userId, productId);
    }

    public Map<String, Object> toggle(int userId, int productId) {
        Product product = productDAO.findById(productId);
        if (product == null || !"AVAILABLE".equals(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        boolean favorite = favoriteDAO.exists(userId, productId);
        if (favorite) {
            favoriteDAO.remove(userId, productId);
            favorite = false;
        } else {
            favoriteDAO.add(userId, productId);
            favorite = true;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("favorite", favorite);
        return data;
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
