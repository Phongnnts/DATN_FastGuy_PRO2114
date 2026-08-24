package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dao.InventoryItemDAO;
import entity.InventoryItem;
import entity.InventoryReservation;
import entity.InventoryReservationItem;
import entity.InventoryTransaction;
import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IngredientOrderInventoryTest {
    @Test
    void reserveConsumeAndReleaseMutateBalancesAndSignedLedger() throws Exception {
        Fixture consumed = fixture(demand(2, "2.0000", 1, "1.5000"));
        consumed.service.reserve(consumed.em, consumed.order, Map.of(1, 1));
        assertEquals(new BigDecimal("1.5000"), consumed.items.get(1).getReservedQuantity());
        assertEquals(new BigDecimal("2.0000"), consumed.items.get(2).getReservedQuantity());
        assertTrue(consumed.service.consume(consumed.em, consumed.order));
        assertEquals(new BigDecimal("8.5000"), consumed.items.get(1).getOnHandQuantity());
        assertEquals(BigDecimal.ZERO.setScale(4), consumed.items.get(1).getReservedQuantity());
        assertEquals(List.of(1, 2, 1, 2), consumed.lockedIds);
        assertEquals(List.of("RESERVE", "RESERVE", "CONSUME", "CONSUME"), consumed.ledgerTypes());
        assertTrue(consumed.ledger.get(2).getQuantity().signum() < 0);
        assertEquals(new BigDecimal("2.0000"), consumed.ledger.get(2).getUnitCostSnapshot());
        assertEquals(new BigDecimal("3.0000"), consumed.ledger.get(2).getTotalCost());

        Fixture released = fixture(demand(1, "1.0000"));
        released.service.reserve(released.em, released.order, Map.of(1, 1));
        assertTrue(released.service.release(released.em, released.order));
        assertEquals(new BigDecimal("10.0000"), released.items.get(1).getOnHandQuantity());
        assertEquals(BigDecimal.ZERO.setScale(4), released.items.get(1).getReservedQuantity());
        assertEquals("RELEASE", released.ledger.get(1).getTransactionType());
    }

    @Test
    void equivalentRetryIsIdempotentAndConflictingRetryRejects() throws Exception {
        Fixture fixture = fixture(demand(1, "1.0000"));
        fixture.service.reserve(fixture.em, fixture.order, Map.of(1, 1));
        int persisted = fixture.persisted.size();

        fixture.service.reserve(fixture.em, fixture.order, Map.of(1, 1));
        assertEquals(persisted, fixture.persisted.size());
        assertThrows(IllegalStateException.class, () -> fixture.service.reserve(fixture.em, fixture.order, Map.of(1, 2)));
    }

    @Test
    void secondReservationCannotOversellAndLocksAscending() throws Exception {
        Fixture fixture = fixture(demand(2, "6.0000", 1, "5.0000"));
        fixture.items.get(2).setOnHandQuantity(new BigDecimal("5.0000"));
        assertThrows(IllegalStateException.class, () -> fixture.service.reserve(fixture.em, fixture.order, Map.of(1, 1)));
        assertEquals(List.of(1, 2), fixture.lockedIds);
        assertTrue(fixture.persisted.isEmpty());
    }

    private Map<Integer, BigDecimal> demand(Object... values) {
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((Integer) values[i], new BigDecimal((String) values[i + 1]));
        return result;
    }

    private Fixture fixture(Map<Integer, BigDecimal> demand) throws Exception {
        Fixture fixture = new Fixture(demand);
        fixture.service = new InventoryReservationService(new InventoryAvailabilityService((em, ids) -> fixture.stocks(demand)), new InventoryItemDAO());
        return fixture;
    }

    private static final class Fixture {
        InventoryReservationService service;
        final Orders order = new Orders();
        final Map<Integer, InventoryItem> items = new LinkedHashMap<>();
        final List<Integer> lockedIds = new ArrayList<>();
        final List<Object> persisted = new ArrayList<>();
        final List<InventoryTransaction> ledger = new ArrayList<>();
        InventoryReservation reservation;
        final EntityManager em;

        Fixture(Map<Integer, BigDecimal> demand) throws Exception {
            set(order, "orderId", 99);
            for (int id : demand.keySet()) {
                InventoryItem item = new InventoryItem();
                set(item, "inventoryItemId", id);
                item.setOnHandQuantity(new BigDecimal("10.0000"));
                item.setAverageUnitCost(new BigDecimal("2.0000"));
                items.put(id, item);
            }
            em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, (proxy, method, args) -> switch (method.getName()) {
                case "find" -> {
                    if (args[0] == InventoryItem.class) { lockedIds.add((Integer) args[1]); yield items.get(args[1]); }
                    yield null;
                }
                case "createQuery" -> queryProxy();
                case "persist" -> { persist(args[0]); yield null; }
                default -> defaultValue(method.getReturnType());
            });
        }

        Map<Integer, InventoryAvailabilityService.VariantStock> stocks(Map<Integer, BigDecimal> demand) {
            List<InventoryAvailabilityService.IngredientStock> ingredients = demand.entrySet().stream().map(e -> new InventoryAvailabilityService.IngredientStock(
                    new InventoryAvailabilityService.ItemStock(e.getKey(), true, items.get(e.getKey()).availableQuantity(), BigDecimal.ZERO), e.getValue())).toList();
            return Map.of(1, new InventoryAvailabilityService.VariantStock(1, "INGREDIENT", new InventoryAvailabilityService.RecipeStock(BigDecimal.ONE, ingredients), null));
        }

        Object queryProxy() {
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{jakarta.persistence.TypedQuery.class}, (proxy, method, args) -> switch (method.getName()) {
                case "setParameter", "setLockMode", "setMaxResults" -> proxy;
                case "getResultList" -> reservation == null ? List.of() : List.of(reservation);
                default -> defaultValue(method.getReturnType());
            });
        }

        void persist(Object value) {
            persisted.add(value);
            if (value instanceof InventoryReservation r) reservation = r;
            if (value instanceof InventoryTransaction t) ledger.add(t);
        }

        List<String> ledgerTypes() { return ledger.stream().map(InventoryTransaction::getTransactionType).toList(); }
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field declared = target.getClass().getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(target, value);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        return 0;
    }
}
