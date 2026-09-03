package service;

import dao.CartDAO;
import dao.ProductDAO;
import dao.ProductModifierDAO;
import entity.Cart;
import entity.CartItem;
import entity.Product;
import entity.ProductModifierOption;
import entity.ProductVariant;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import utils.DatabaseUtil;

public class CartService {

    private CartDAO cartDAO = new CartDAO();
    private ProductDAO productDAO = new ProductDAO();
    private ProductModifierDAO modifierDAO = new ProductModifierDAO();
    private InventoryAvailabilityService inventoryAvailabilityService =
        new InventoryAvailabilityService();

    static Integer availabilityLimit(
        InventoryAvailabilityService.AvailabilityResult availability
    ) {
        return "UNTRACKED".equals(availability.mode())
            ? null
            : availability.servings() == null
              ? 0
              : availability.servings();
    }

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
        List<Integer> variantIds = items
            .stream()
            .map(CartItem::getVariant)
            .filter(java.util.Objects::nonNull)
            .map(ProductVariant::getVariantId)
            .distinct()
            .toList();
        Map<Integer, Map<String, Object>> availability =
            inventoryAvailabilityService.publicAvailability(variantIds);

        List<Map<String, Object>> itemList = items
            .stream()
            .map(ci -> {
                Map<String, Object> m = new HashMap<>();
                m.put("cartItemId", ci.getCartItemId());
                m.put("productId", ci.getProduct().getProductId());
                m.put(
                    "variantId",
                    ci.getVariant() != null
                        ? ci.getVariant().getVariantId()
                        : null
                );
                m.put("productName", ci.getProduct().getName());
                m.put(
                    "variantName",
                    ci.getVariant() != null
                        ? ci.getVariant().getVariantName()
                        : ""
                );
                m.put("imageUrl", ci.getProduct().getImageUrl());
                m.put("quantity", ci.getQuantity());
                m.put("unitPrice", ci.getUnitPrice());
                m.put(
                    "quantityAvailable",
                    ci.getVariant() != null
                        ? ci.getVariant().getQuantityAvailable()
                        : null
                );
                m.put(
                    "inventoryMode",
                    ci.getVariant() != null
                        ? ci.getVariant().getInventoryMode()
                        : null
                );
                m.put(
                    "remainingServings",
                    ci.getVariant() != null
                        ? availability
                              .getOrDefault(
                                  ci.getVariant().getVariantId(),
                                  Map.of()
                              )
                              .get("remainingServings")
                        : null
                );
                m.put(
                    "variantStatus",
                    ci.getVariant() != null
                        ? ci.getVariant().getStatus()
                        : "UNAVAILABLE"
                );
                m.put("productStatus", ci.getProduct().getStatus());
                List<Map<String, Object>> modifiers = new ArrayList<>();
                if (ci.getModifiers() != null) {
                    for (var mod : ci.getModifiers()) {
                        modifiers.add(
                            Map.of(
                                "modifierOptionId",
                                mod.modifierOptionId,
                                "groupName",
                                mod.groupName != null ? mod.groupName : "",
                                "name",
                                mod.name != null ? mod.name : "",
                                "price",
                                mod.price != null
                                    ? mod.price
                                    : java.math.BigDecimal.ZERO
                            )
                        );
                    }
                }
                m.put("modifiers", modifiers);
                return m;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cart.getCartId());
        result.put("items", itemList);
        return result;
    }

    public boolean addItem(
        User user,
        int productId,
        int variantId,
        int quantity,
        List<Integer> modifierOptionIds
    ) {
        if (quantity <= 0) return false;
        List<Integer> optionIds =
            modifierOptionIds != null
                ? modifierOptionIds
                      .stream()
                      .distinct()
                      .collect(Collectors.toList())
                : List.of();
        String modifierKey = optionIds
            .stream()
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        Cart cart = getCartByUser(user);
        List<CartItem> items =
            cart == null ? List.of() : cartDAO.getItems(cart.getCartId());
        int productQuantity =
            items
                .stream()
                .filter(ci -> ci.getProduct().getProductId() == productId)
                .mapToInt(CartItem::getQuantity)
                .sum() + quantity;
        if (
            !OrderQuantityPolicy.allows(productQuantity)
        ) throw new IllegalArgumentException(OrderQuantityPolicy.MESSAGE);
        CartItem existing = items
            .stream()
            .filter(
                ci ->
                    ci.getProduct().getProductId() == productId &&
                    ci.getVariant() != null &&
                    ci.getVariant().getVariantId() == variantId &&
                    modifierKey.equals(getModifierKey(ci))
            )
            .findFirst()
            .orElse(null);
        int newQty =
            existing == null ? quantity : existing.getQuantity() + quantity;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(
                ProductVariant.class,
                variantId,
                LockModeType.PESSIMISTIC_WRITE
            );
            if (
                variant == null ||
                variant.getProduct() == null ||
                variant.getProduct().getProductId() != productId
            ) {
                em.getTransaction().rollback();
                return false;
            }
            if (!"AVAILABLE".equals(variant.getStatus())) {
                em.getTransaction().rollback();
                return false;
            }

            BigDecimal modifierPrice = BigDecimal.ZERO;
            Map<Integer, Integer> selectedByGroup = new HashMap<>();
            for (Integer optionId : optionIds) {
                var option = modifierDAO.option(optionId);
                if (
                    option == null ||
                    !Boolean.TRUE.equals(option.getIsActive()) ||
                    option.getGroup() == null ||
                    option.getGroup().getProduct().getProductId() !=
                        productId ||
                    !Boolean.TRUE.equals(option.getGroup().getIsActive())
                ) {
                    em.getTransaction().rollback();
                    return false;
                }
                selectedByGroup.merge(
                    option.getGroup().getModifierGroupId(),
                    1,
                    Integer::sum
                );
                modifierPrice = modifierPrice.add(
                    option.getPrice() != null
                        ? option.getPrice()
                        : BigDecimal.ZERO
                );
            }
            if (!optionIds.isEmpty()) {
                for (var group : modifierDAO.groups(productId)) {
                    if (Boolean.TRUE.equals(group.getIsActive())) {
                        int selected = selectedByGroup.getOrDefault(
                            group.getModifierGroupId(),
                            0
                        );
                        if (
                            selected < group.getMinSelections() ||
                            selected > group.getMaxSelections()
                        ) {
                            em.getTransaction().rollback();
                            return false;
                        }
                    }
                }
            }

            Integer stock = availabilityLimit(
                inventoryAvailabilityService.availability(em, variantId)
            );
            if (stock != null && stock < newQty) {
                em.getTransaction().rollback();
                return false;
            }
            em.getTransaction().commit();

            if (cart == null) cart = getOrCreateCart(user);

            if (existing != null) {
                existing.setQuantity(newQty);
                cartDAO.updateItemQuantity(existing.getCartItemId(), newQty);
            } else {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(variant.getProduct());
                item.setVariant(variant);
                item.setQuantity(quantity);
                item.setUnitPrice(
                    (variant.getPrice() != null
                        ? variant.getPrice()
                        : variant.getProduct().getBasePrice()
                    ).add(modifierPrice)
                );
                item.setCreatedAt(LocalDateTime.now());
                List<CartItem.ModifierItem> modifierItems = new ArrayList<>();
                for (Integer optionId : optionIds) {
                    ProductModifierOption option = modifierDAO.option(optionId);
                    if (option != null && option.getGroup() != null) {
                        modifierItems.add(
                            new CartItem.ModifierItem(
                                option.getModifierOptionId(),
                                option.getGroup().getModifierGroupId(),
                                option.getGroup().getName(),
                                option.getName(),
                                option.getPrice()
                            )
                        );
                    }
                }
                item.setModifiers(modifierItems);
                cartDAO.addItem(item);
            }
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private String getModifierKey(CartItem ci) {
        if (ci.getModifiers() == null || ci.getModifiers().isEmpty()) return "";
        return ci
            .getModifiers()
            .stream()
            .map(m -> String.valueOf(m.modifierOptionId))
            .sorted()
            .collect(Collectors.joining(","));
    }

    public boolean updateItemQuantity(
        int cartItemId,
        int userId,
        int quantity
    ) {
        Cart cart = getCartByUser(
            new User() {
                {
                    setUserId(userId);
                }
            }
        );
        if (cart == null) return false;

        List<CartItem> items = cartDAO.getItems(cart.getCartId());
        CartItem item = items
            .stream()
            .filter(ci -> ci.getCartItemId() == cartItemId)
            .findFirst()
            .orElse(null);
        if (item == null) return false;
        int productQuantity =
            items
                .stream()
                .filter(
                    ci ->
                        ci.getProduct().getProductId() ==
                            item.getProduct().getProductId() &&
                        ci.getCartItemId() != cartItemId
                )
                .mapToInt(CartItem::getQuantity)
                .sum() + Math.max(0, quantity);
        if (
            !OrderQuantityPolicy.allows(productQuantity)
        ) throw new IllegalArgumentException(OrderQuantityPolicy.MESSAGE);

        if (quantity <= 0) {
            cartDAO.removeItem(cartItemId);
            return true;
        }

        if (item.getVariant() != null) {
            EntityManager em = DatabaseUtil.getEntityManager();
            try {
                em.getTransaction().begin();
                ProductVariant locked = em.find(
                    ProductVariant.class,
                    item.getVariant().getVariantId(),
                    LockModeType.PESSIMISTIC_WRITE
                );
                Integer stock =
                    locked == null
                        ? 0
                        : availabilityLimit(
                              inventoryAvailabilityService.availability(
                                  em,
                                  locked.getVariantId()
                              )
                          );
                if (stock != null && stock < quantity) {
                    em.getTransaction().rollback();
                    return false;
                }
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (
                    em.getTransaction().isActive()
                ) em.getTransaction().rollback();
                throw e;
            } finally {
                em.close();
            }
        }

        cartDAO.updateItemQuantity(cartItemId, quantity);
        return true;
    }

    public boolean removeItem(int cartItemId, int userId) {
        Cart cart = getCartByUser(
            new User() {
                {
                    setUserId(userId);
                }
            }
        );
        if (cart == null) return false;

        List<CartItem> items = cartDAO.getItems(cart.getCartId());
        boolean belongs = items
            .stream()
            .anyMatch(ci -> ci.getCartItemId() == cartItemId);
        if (!belongs) return false;

        cartDAO.removeItem(cartItemId);
        return true;
    }
}
