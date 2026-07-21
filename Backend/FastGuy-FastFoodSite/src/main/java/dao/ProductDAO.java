package dao;

import entity.Product;
import entity.ProductVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import utils.DatabaseUtil;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDAO {
    public Product findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Product.class, id);
        } finally {
            em.close();
        }
    }

    public List<Product> findByCategoryId(int categoryId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.category.categoryId = :cid AND p.status = 'AVAILABLE' ORDER BY p.productId",
                    Product.class)
                    .setParameter("cid", categoryId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Product> findAllAvailable() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.status = 'AVAILABLE' ORDER BY p.productId",
                    Product.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Product> search(String q, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                String availability, String productType, Boolean discounted, Long minSold,
                                String sort, Integer page, Integer size) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String where = buildWhere(q, categoryId, minPrice, maxPrice, availability, productType, discounted, minSold);
            String price = effectivePrice();
            String order = switch (sort) {
                case "name" -> "p.name ASC, p.productId ASC";
                case "name-desc" -> "p.name DESC, p.productId DESC";
                case "newest" -> "p.createdAt DESC, p.productId DESC";
                case "price-asc" -> price + " ASC, p.productId ASC";
                case "price-desc" -> price + " DESC, p.productId DESC";
                case "best-selling" -> soldCount() + " DESC, p.productId ASC";
                case "discount-desc" -> discountPercent() + " DESC, p.productId ASC";
                default -> "p.productId ASC";
            };
            TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p WHERE " + where + " ORDER BY " + order, Product.class);
            setSearchParameters(query, q, categoryId, minPrice, maxPrice, availability, minSold);
            if (page != null) query.setFirstResult(page * size).setMaxResults(size);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public long countSearch(String q, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String availability,
                            String productType, Boolean discounted, Long minSold) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(p) FROM Product p WHERE " + buildWhere(q, categoryId, minPrice, maxPrice, availability, productType, discounted, minSold), Long.class);
            setSearchParameters(query, q, categoryId, minPrice, maxPrice, availability, minSold);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    private String effectivePrice() {
        return "COALESCE((SELECT v.price FROM ProductVariant v WHERE v.product = p AND v.isDefault = true AND v.variantId = (SELECT MIN(d.variantId) FROM ProductVariant d WHERE d.product = p AND d.isDefault = true)), p.basePrice)";
    }

    private String soldCount() {
        return "(SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.product = p AND oi.order.orderStatus = 'DELIVERED' AND oi.order.paymentStatus = 'PAID')";
    }

    private String discountPercent() {
        return "COALESCE((SELECT (v.originalPrice - v.price) / v.originalPrice FROM ProductVariant v WHERE v.product = p AND v.isDefault = true AND v.originalPrice > v.price AND v.originalPrice > 0 AND v.variantId = (SELECT MIN(d.variantId) FROM ProductVariant d WHERE d.product = p AND d.isDefault = true)), 0)";
    }

    private String buildWhere(String q, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String availability,
                              String productType, Boolean discounted, Long minSold) {
        String price = effectivePrice();
        StringBuilder where = new StringBuilder("p.status = 'AVAILABLE'");
        if (q != null) where.append(" AND (LOWER(p.name) LIKE :q OR LOWER(COALESCE(p.description, '')) LIKE :q OR EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND LOWER(COALESCE(v.sku, '')) LIKE :q))");
        if (categoryId != null) where.append(" AND p.category.categoryId = :categoryId");
        if (minPrice != null) where.append(" AND ").append(price).append(" >= :minPrice");
        if (maxPrice != null) where.append(" AND ").append(price).append(" <= :maxPrice");
        String variants = "EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND (v.isDefault = false OR v.variantId <> (SELECT MIN(d.variantId) FROM ProductVariant d WHERE d.product = p)))";
        String modifiers = "EXISTS (SELECT g FROM ProductModifierGroup g WHERE g.product = p AND g.isActive = true)";
        String combo = "EXISTS (SELECT c FROM ProductCombo c WHERE c.product = p AND c.isActive = true)";
        if ("SIMPLE".equals(productType)) where.append(" AND NOT ").append(variants).append(" AND NOT ").append(modifiers).append(" AND NOT ").append(combo);
        if ("VARIANT".equals(productType)) where.append(" AND ").append(variants).append(" AND NOT ").append(combo);
        if ("COMBO".equals(productType)) where.append(" AND ").append(combo);
        if ("CUSTOMIZABLE".equals(productType)) where.append(" AND ").append(modifiers).append(" AND NOT ").append(combo);
        String hasDiscount = "EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND v.isDefault = true AND v.originalPrice IS NOT NULL AND v.originalPrice > v.price)";
        if (Boolean.TRUE.equals(discounted)) where.append(" AND ").append(hasDiscount);
        if (Boolean.FALSE.equals(discounted)) where.append(" AND NOT ").append(hasDiscount);
        if (minSold != null) where.append(" AND ").append(soldCount()).append(" >= :minSold");
        String withinHours = "(p.availableFrom IS NULL AND p.availableTo IS NULL OR p.availableFrom IS NULL AND :now < p.availableTo OR p.availableTo IS NULL AND :now >= p.availableFrom OR p.availableFrom < p.availableTo AND :now >= p.availableFrom AND :now < p.availableTo OR p.availableFrom >= p.availableTo AND (:now >= p.availableFrom OR :now < p.availableTo))";
        if ("AVAILABLE".equals(availability)) where.append(" AND EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND v.status = 'AVAILABLE' AND (v.quantityAvailable IS NULL OR v.quantityAvailable > 0)) AND ").append(withinHours);
        if ("OUT_OF_STOCK".equals(availability)) where.append(" AND NOT EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND v.status = 'AVAILABLE' AND (v.quantityAvailable IS NULL OR v.quantityAvailable > 0)) AND ").append(withinHours);
        if ("OUTSIDE_HOURS".equals(availability)) where.append(" AND NOT ").append(withinHours);
        return where.toString();
    }

    private void setSearchParameters(TypedQuery<?> query, String q, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String availability, Long minSold) {
        if (q != null) query.setParameter("q", "%" + q.toLowerCase() + "%");
        if (categoryId != null) query.setParameter("categoryId", categoryId);
        if (minPrice != null) query.setParameter("minPrice", minPrice);
        if (maxPrice != null) query.setParameter("maxPrice", maxPrice);
        if (minSold != null) query.setParameter("minSold", minSold);
        if (!"ALL".equals(availability)) query.setParameter("now", LocalTime.now());
    }

    public Map<Integer, Long> soldCounts(List<Integer> productIds) {
        Map<Integer, Long> result = new HashMap<>();
        if (productIds.isEmpty()) return result;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.createQuery("SELECT oi.product.productId, SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.productId IN :ids AND oi.order.orderStatus = 'DELIVERED' AND oi.order.paymentStatus = 'PAID' GROUP BY oi.product.productId", Object[].class)
                    .setParameter("ids", productIds).getResultList()
                    .forEach(row -> result.put((Integer) row[0], (Long) row[1]));
            return result;
        } finally {
            em.close();
        }
    }

    public Map<Integer, Integer> featureFlags(List<Integer> productIds) {
        Map<Integer, Integer> result = new HashMap<>();
        if (productIds.isEmpty()) return result;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.createQuery("SELECT p.productId, CASE WHEN EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND (v.isDefault = false OR v.variantId <> (SELECT MIN(d.variantId) FROM ProductVariant d WHERE d.product = p))) THEN 1 ELSE 0 END, CASE WHEN EXISTS (SELECT g FROM ProductModifierGroup g WHERE g.product = p AND g.isActive = true) THEN 1 ELSE 0 END, CASE WHEN EXISTS (SELECT c FROM ProductCombo c WHERE c.product = p AND c.isActive = true) THEN 1 ELSE 0 END FROM Product p WHERE p.productId IN :ids", Object[].class)
                    .setParameter("ids", productIds).getResultList().forEach(row -> result.put((Integer) row[0], ((Number) row[1]).intValue() | (((Number) row[2]).intValue() << 1) | (((Number) row[3]).intValue() << 2)));
            return result;
        } finally {
            em.close();
        }
    }

    public Map<Integer, ProductVariant> defaultVariants(List<Integer> productIds) {
        Map<Integer, ProductVariant> result = new HashMap<>();
        if (productIds.isEmpty()) return result;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.createQuery("SELECT v FROM ProductVariant v WHERE v.product.productId IN :ids AND v.isDefault = true ORDER BY v.variantId", ProductVariant.class)
                    .setParameter("ids", productIds).getResultList().forEach(v -> result.putIfAbsent(v.getProduct().getProductId(), v));
            return result;
        } finally {
            em.close();
        }
    }

    public Map<Integer, Long> countAvailableByCategory() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Map<Integer, Long> counts = new HashMap<>();
            em.createQuery("SELECT p.category.categoryId, COUNT(p) FROM Product p WHERE p.status = 'AVAILABLE' GROUP BY p.category.categoryId", Object[].class)
                    .getResultList().forEach(row -> counts.put((Integer) row[0], (Long) row[1]));
            return counts;
        } finally {
            em.close();
        }
    }

    public List<Product> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Product p ORDER BY p.productId", Product.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<ProductVariant> findVariantsByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT v FROM ProductVariant v WHERE v.product.productId = :pid ORDER BY v.isDefault DESC, v.variantId",
                    ProductVariant.class)
                    .setParameter("pid", productId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public ProductVariant findDefaultVariantByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<ProductVariant> result = em.createQuery(
                    "SELECT v FROM ProductVariant v WHERE v.product.productId = :pid AND v.isDefault = true ORDER BY v.variantId",
                    ProductVariant.class)
                    .setParameter("pid", productId)
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    public ProductVariant findVariantById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(ProductVariant.class, id);
        } finally {
            em.close();
        }
    }

    public long count() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public void save(Product product) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (product.getProductId() == 0) {
                em.persist(product);
            } else {
                em.merge(product);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save product: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Product p = em.find(Product.class, id);
            if (p != null) em.remove(p);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to delete product: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void saveVariant(ProductVariant variant) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (variant.getVariantId() == 0) {
                em.persist(variant);
            } else {
                em.merge(variant);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save variant: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void deleteVariant(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            ProductVariant v = em.find(ProductVariant.class, id);
            if (v != null) em.remove(v);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to delete variant: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
