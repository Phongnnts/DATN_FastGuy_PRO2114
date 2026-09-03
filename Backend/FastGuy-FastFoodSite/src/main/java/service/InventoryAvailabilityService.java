package service;

import entity.ProductVariant;
import entity.Recipe;
import entity.RecipeItem;
import entity.VariantInventoryItem;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import utils.DatabaseUtil;

public class InventoryAvailabilityService {

    public record AvailabilityResult(
        String mode,
        String status,
        Integer servings,
        Integer limitingItemId
    ) {}

    public record ItemStock(
        int id,
        String itemType,
        boolean active,
        BigDecimal available,
        BigDecimal minimum
    ) {}

    public record IngredientStock(ItemStock item, BigDecimal quantity) {}

    public record RecipeStock(
        BigDecimal yieldQuantity,
        List<IngredientStock> ingredients
    ) {}

    public record VariantStock(
        int id,
        String mode,
        RecipeStock recipe,
        ItemStock finishedGood
    ) {}

    @FunctionalInterface
    interface StockLoader {
        Map<Integer, VariantStock> load(
            EntityManager em,
            List<Integer> variantIds
        );
    }

    private static final BigDecimal MAX_DECIMAL = new BigDecimal(
        "999999999999999.9999"
    );
    private static final int MAX_ORDER_QUANTITY = 1_000_000;
    private static final Set<String> MODES = Set.of(
        "INGREDIENT",
        "FINISHED_GOOD",
        "UNTRACKED",
        "SUSPENDED"
    );
    private final StockLoader loader;

    public InventoryAvailabilityService() {
        this(InventoryAvailabilityService::loadStocks);
    }

    InventoryAvailabilityService(StockLoader loader) {
        this.loader = loader;
    }

    public Map<Integer, BigDecimal> aggregateDemand(
        EntityManager em,
        Map<Integer, Integer> variantQuantities
    ) {
        List<Integer> ids = validate(variantQuantities);
        Map<Integer, VariantStock> stocks = loader.load(em, ids);
        Map<Integer, BigDecimal> exact = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : variantQuantities.entrySet()) {
            VariantStock stock = requireStock(stocks, entry.getKey());
            int ordered = entry.getValue();
            switch (stock.mode()) {
                case "INGREDIENT" -> {
                    RecipeStock recipe = stock.recipe();
                    if (!validRecipe(recipe)) throw new IllegalStateException(
                        "Active recipe with active items required"
                    );
                    for (IngredientStock ingredient : recipe.ingredients()) {
                        BigDecimal contribution = ingredient
                            .quantity()
                            .multiply(
                                BigDecimal.valueOf(ordered),
                                MathContext.DECIMAL128
                            )
                            .divide(
                                recipe.yieldQuantity(),
                                MathContext.DECIMAL128
                            );
                        exact.merge(
                            ingredient.item().id(),
                            contribution,
                            (a, b) -> a.add(b, MathContext.DECIMAL128)
                        );
                    }
                }
                case "FINISHED_GOOD" -> {
                    if (
                        !validFinishedGood(stock.finishedGood())
                    ) throw new IllegalStateException(
                        "Active finished-good item required"
                    );
                    exact.merge(
                        stock.finishedGood().id(),
                        BigDecimal.valueOf(ordered),
                        BigDecimal::add
                    );
                }
                case "UNTRACKED" -> {
                }
                case "SUSPENDED" -> throw new IllegalStateException(
                    "Variant inventory unavailable"
                );
                default -> throw new IllegalStateException(
                    "Unknown inventory mode"
                );
            }
        }
        Map<Integer, BigDecimal> result = new HashMap<>();
        exact.forEach((id, value) -> result.put(id, decimal(value)));
        return result;
    }

    public Map<Integer, Map<String, Object>> publicAvailability(
        List<Integer> variantIds
    ) {
        if (variantIds != null && variantIds.isEmpty()) return Map.of();
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return publicAvailability(em, variantIds);
        } finally {
            em.close();
        }
    }

    Map<Integer, Map<String, Object>> publicAvailability(
        EntityManager em,
        List<Integer> variantIds
    ) {
        if (
            variantIds == null ||
            variantIds.stream().anyMatch(id -> id == null || id <= 0)
        ) throw new IllegalArgumentException("Invalid variant ID");
        if (variantIds.isEmpty()) return Map.of();
        Map<Integer, VariantStock> stocks = loader.load(
            em,
            variantIds.stream().distinct().sorted().toList()
        );
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (int id : variantIds) {
            VariantStock stock = stocks == null ? null : stocks.get(id);
            result.put(
                id,
                stock == null ||
                    stock.mode() == null ||
                    !MODES.contains(stock.mode())
                    ? Map.of("availabilityStatus", "OUT_OF_STOCK")
                    : publicResult(availability(stock))
            );
        }
        return result;
    }

    public AvailabilityResult availability(EntityManager em, int variantId) {
        if (variantId <= 0) throw new IllegalArgumentException(
            "Invalid variant ID"
        );
        return availability(
            requireStock(loader.load(em, List.of(variantId)), variantId)
        );
    }

    private AvailabilityResult availability(VariantStock stock) {
        return switch (stock.mode()) {
            case "UNTRACKED" -> new AvailabilityResult(
                stock.mode(),
                "AVAILABLE",
                null,
                null
            );
            case "SUSPENDED" -> new AvailabilityResult(
                stock.mode(),
                "SUSPENDED",
                0,
                null
            );
            case "FINISHED_GOOD" -> validFinishedGood(stock.finishedGood())
                ? result(
                      stock.mode(),
                      List.of(
                          new Capacity(stock.finishedGood(), BigDecimal.ONE)
                      )
                  )
                : new AvailabilityResult(stock.mode(), "UNAVAILABLE", 0, null);
            case "INGREDIENT" -> validRecipe(stock.recipe())
                ? result(
                      stock.mode(),
                      stock
                          .recipe()
                          .ingredients()
                          .stream()
                          .map(i ->
                              new Capacity(
                                  i.item(),
                                  i
                                      .quantity()
                                      .divide(
                                          stock.recipe().yieldQuantity(),
                                          MathContext.DECIMAL128
                                      )
                              )
                          )
                          .toList()
                  )
                : new AvailabilityResult(stock.mode(), "UNAVAILABLE", 0, null);
            default -> throw new IllegalStateException(
                "Unknown inventory mode"
            );
        };
    }

    private Map<String, Object> publicResult(AvailabilityResult availability) {
        int servings =
            availability.servings() == null ? 0 : availability.servings();
        String status = switch (availability.mode()) {
            case "UNTRACKED", "SUSPENDED" -> availability.mode();
            default -> servings == 0
                ? "OUT_OF_STOCK"
                : servings <= 3
                  ? "LOW_STOCK"
                  : "IN_STOCK";
        };
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availabilityStatus", status);
        if (
            servings > 0 && !Set.of("UNTRACKED", "SUSPENDED").contains(status)
        ) result.put("remainingServings", servings);
        return result;
    }

    private AvailabilityResult result(String mode, List<Capacity> capacities) {
        Capacity limiting = capacities
            .stream()
            .min((a, b) -> Integer.compare(a.servings(), b.servings()))
            .orElseThrow();
        int servings = limiting.servings();
        String status =
            servings == 0
                ? "OUT_OF_STOCK"
                : limiting
                        .item()
                        .available()
                        .compareTo(limiting.item().minimum()) <= 0
                  ? "LOW_STOCK"
                  : "AVAILABLE";
        return new AvailabilityResult(
            mode,
            status,
            servings,
            limiting.item().id()
        );
    }

    private static List<Integer> validate(Map<Integer, Integer> quantities) {
        if (
            quantities == null || quantities.isEmpty()
        ) throw new IllegalArgumentException("Variant quantities required");
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            if (
                entry.getKey() == null || entry.getKey() <= 0
            ) throw new IllegalArgumentException("Invalid variant ID");
            if (
                entry.getValue() == null ||
                entry.getValue() <= 0 ||
                entry.getValue() > MAX_ORDER_QUANTITY
            ) throw new IllegalArgumentException("Invalid variant quantity");
        }
        return quantities.keySet().stream().sorted().toList();
    }

    private static VariantStock requireStock(
        Map<Integer, VariantStock> stocks,
        int id
    ) {
        VariantStock stock = stocks == null ? null : stocks.get(id);
        if (stock == null) throw new IllegalArgumentException(
            "Variant not found"
        );
        if (
            stock.mode() == null || !MODES.contains(stock.mode())
        ) throw new IllegalStateException("Unknown inventory mode");
        return stock;
    }

    private static boolean validRecipe(RecipeStock recipe) {
        return (
            recipe != null &&
            recipe.yieldQuantity() != null &&
            recipe.yieldQuantity().compareTo(BigDecimal.ZERO) > 0 &&
            recipe.ingredients() != null &&
            !recipe.ingredients().isEmpty() &&
            recipe
                .ingredients()
                .stream()
                .allMatch(
                    i ->
                        i != null &&
                        i.quantity() != null &&
                        i.quantity().compareTo(BigDecimal.ZERO) > 0 &&
                        validIngredient(i.item())
                )
        );
    }

    private static boolean validItem(ItemStock item) {
        return (
            item != null &&
            item.active() &&
            item.available() != null &&
            item.available().compareTo(BigDecimal.ZERO) >= 0 &&
            item.minimum() != null &&
            item.minimum().compareTo(BigDecimal.ZERO) >= 0
        );
    }

    private static boolean validFinishedGood(ItemStock item) {
        return validItem(item) && "FINISHED_GOOD".equals(item.itemType());
    }

    private static boolean validIngredient(ItemStock item) {
        return validItem(item) && "INGREDIENT".equals(item.itemType());
    }

    private static BigDecimal decimal(BigDecimal value) {
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (
            normalized.abs().compareTo(MAX_DECIMAL) > 0
        ) throw new ArithmeticException("DECIMAL(19,4) overflow");
        return normalized;
    }

    private record Capacity(ItemStock item, BigDecimal perServing) {
        int servings() {
            BigDecimal value = item
                .available()
                .divide(perServing, 0, RoundingMode.FLOOR);
            return value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0
                ? Integer.MAX_VALUE
                : value.intValue();
        }
    }

    private static Map<Integer, VariantStock> loadStocks(
        EntityManager em,
        List<Integer> ids
    ) {
        List<ProductVariant> variants = em
            .createQuery(
                "SELECT v FROM ProductVariant v WHERE v.variantId IN :ids",
                ProductVariant.class
            )
            .setParameter("ids", ids)
            .getResultList();
        List<Recipe> recipes = em
            .createQuery(
                "SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem WHERE r.variant.variantId IN :ids AND r.active = true",
                Recipe.class
            )
            .setParameter("ids", ids)
            .getResultList();
        List<VariantInventoryItem> mappings = em
            .createQuery(
                "SELECT m FROM VariantInventoryItem m JOIN FETCH m.inventoryItem WHERE m.variant.variantId IN :ids",
                VariantInventoryItem.class
            )
            .setParameter("ids", ids)
            .getResultList();
        Map<Integer, RecipeStock> recipeByVariant = new HashMap<>();
        for (Recipe recipe : recipes)
            recipeByVariant.put(
                recipe.getVariant().getVariantId(),
                new RecipeStock(
                    recipe.getYieldQuantity(),
                    recipe
                        .getItems()
                        .stream()
                        .map(InventoryAvailabilityService::ingredient)
                        .toList()
                )
            );
        Map<Integer, ItemStock> itemByVariant = new HashMap<>();
        for (VariantInventoryItem mapping : mappings)
            itemByVariant.put(
                mapping.getVariant().getVariantId(),
                item(mapping.getInventoryItem())
            );
        Map<Integer, VariantStock> result = new LinkedHashMap<>();
        for (ProductVariant variant : variants)
            result.put(
                variant.getVariantId(),
                new VariantStock(
                    variant.getVariantId(),
                    variant.getInventoryMode(),
                    recipeByVariant.get(variant.getVariantId()),
                    itemByVariant.get(variant.getVariantId())
                )
            );
        return result;
    }

    private static IngredientStock ingredient(RecipeItem ingredient) {
        return new IngredientStock(
            item(ingredient.getInventoryItem()),
            ingredient.getQuantity()
        );
    }

    private static ItemStock item(entity.InventoryItem item) {
        return new ItemStock(
            item.getInventoryItemId(),
            item.getItemType(),
            item.isActive(),
            item.availableQuantity(),
            item.getMinimumQuantity()
        );
    }
}
