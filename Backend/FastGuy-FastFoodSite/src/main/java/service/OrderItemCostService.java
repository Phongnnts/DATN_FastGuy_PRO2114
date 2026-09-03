package service;

import entity.*;
import jakarta.persistence.EntityManager;
import java.math.*;
import java.util.*;

public class OrderItemCostService {

    public record CostLine(BigDecimal quantity, BigDecimal unitCost) {}

    public static BigDecimal recipeUnitCost(
        BigDecimal yield,
        List<CostLine> lines
    ) {
        if (
            yield == null ||
            yield.signum() <= 0 ||
            lines == null ||
            lines.isEmpty()
        ) return null;
        BigDecimal total = BigDecimal.ZERO;
        for (CostLine line : lines) {
            if (
                line == null ||
                line.quantity() == null ||
                line.quantity().signum() <= 0 ||
                line.unitCost() == null ||
                line.unitCost().signum() <= 0
            ) return null;
            total = total.add(line.quantity().multiply(line.unitCost()));
        }
        return total.divide(yield, 4, RoundingMode.HALF_UP);
    }

    public void snapshot(EntityManager em, Orders order) {
        List<OrderItem> orderItems = em
            .createQuery(
                "SELECT oi FROM OrderItem oi JOIN FETCH oi.variant WHERE oi.order.orderId=:orderId ORDER BY oi.orderItemId",
                OrderItem.class
            )
            .setParameter("orderId", order.getOrderId())
            .getResultList();
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getUnitCostSnapshot() != null) continue;
            BigDecimal unitCost = variantCost(em, orderItem.getVariant());
            if (unitCost == null) continue;
            BigDecimal moneyCost = unitCost.setScale(2, RoundingMode.HALF_UP);
            orderItem.setUnitCostSnapshot(moneyCost);
            orderItem.setTotalCostSnapshot(
                moneyCost
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP)
            );
        }
    }

    private BigDecimal variantCost(EntityManager em, ProductVariant variant) {
        if (variant == null) return null;
        if ("UNTRACKED".equals(variant.getInventoryMode())) return null;
        if ("FINISHED_GOOD".equals(variant.getInventoryMode())) {
            List<VariantInventoryItem> mappings = em
                .createQuery(
                    "SELECT m FROM VariantInventoryItem m JOIN FETCH m.inventoryItem WHERE m.variant.variantId=:variantId",
                    VariantInventoryItem.class
                )
                .setParameter("variantId", variant.getVariantId())
                .getResultList();
            if (mappings.size() != 1) return null;
            BigDecimal cost = mappings
                .get(0)
                .getInventoryItem()
                .getAverageUnitCost();
            return cost != null && cost.signum() > 0 ? cost : null;
        }
        if (!"INGREDIENT".equals(variant.getInventoryMode())) return null;
        List<Recipe> recipes = em
            .createQuery(
                "SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem WHERE r.variant.variantId=:variantId AND r.active=true",
                Recipe.class
            )
            .setParameter("variantId", variant.getVariantId())
            .getResultList();
        if (recipes.size() != 1) return null;
        Recipe recipe = recipes.get(0);
        return recipeUnitCost(
            recipe.getYieldQuantity(),
            recipe
                .getItems()
                .stream()
                .map(line ->
                    new CostLine(
                        line.getQuantity(),
                        line.getInventoryItem().getAverageUnitCost()
                    )
                )
                .toList()
        );
    }
}
