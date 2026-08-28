package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dao.InventoryItemDAO;
import entity.InventoryItem;
import entity.InventoryReservation;
import entity.InventoryReservationItem;
import entity.InventoryTransaction;
import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

public class InventoryReservationService {
    private final InventoryAvailabilityService availability;
    private final InventoryItemDAO items;

    public InventoryReservationService() { this(new InventoryAvailabilityService(), new InventoryItemDAO()); }
    InventoryReservationService(InventoryAvailabilityService availability, InventoryItemDAO items) {
        this.availability = availability;
        this.items = items;
    }

    public static boolean canTransition(String from, String to) {
        return "RESERVED".equals(from) && Set.of("CONSUMED", "RELEASED").contains(to)
                || "CONSUMED".equals(from) && "WASTED".equals(to);
    }

    public void reserve(EntityManager em, Orders order, Map<Integer, Integer> variantQuantities) {
        Map<Integer, BigDecimal> demand = availability.aggregateDemand(em, variantQuantities);
        InventoryReservation existing = findByOrder(em, order.getOrderId());
        if (existing != null) {
            if (equivalent(existing, demand)) return;
            throw new IllegalStateException("Conflicting order reservation");
        }
        List<Integer> itemIds = demand.keySet().stream().sorted().toList();
        List<InventoryItem> locked = itemIds.stream().map(id -> items.lock(em, id)).toList();
        if (locked.stream().anyMatch(i -> i == null || !i.isActive())) throw new IllegalStateException("Inventory item unavailable");
        for (InventoryItem item : locked) if (demand.get(item.getInventoryItemId()).compareTo(item.availableQuantity()) > 0) throw new IllegalStateException("Insufficient inventory");
        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrder(order);
        reservation.setStatus("RESERVED");
        em.persist(reservation);
        for (InventoryItem item : locked) {
            BigDecimal quantity = scale(demand.get(item.getInventoryItemId()));
            BigDecimal before = item.getReservedQuantity();
            item.reserve(quantity);
            InventoryReservationItem line = new InventoryReservationItem();
            line.setReservation(reservation);
            line.setInventoryItem(item);
            line.setQuantity(quantity);
            reservation.getItems().add(line);
            em.persist(line);
            record(em, order, item, "RESERVE", quantity, before, item.getReservedQuantity());
        }
    }

    public boolean consume(EntityManager em, Orders order) {
        InventoryReservation reservation = findByOrder(em, order.getOrderId());
        if (reservation == null || !canTransition(reservation.getStatus(), "CONSUMED")) return false;
        for (InventoryReservationItem line : reservation.getItems().stream().sorted((a, b) -> Integer.compare(a.getInventoryItem().getInventoryItemId(), b.getInventoryItem().getInventoryItemId())).toList()) {
            InventoryItem item = items.lock(em, line.getInventoryItem().getInventoryItemId());
            BigDecimal before = item.getOnHandQuantity();
            item.release(line.getQuantity());
            item.setOnHandQuantity(item.getOnHandQuantity().subtract(line.getQuantity()));
            record(em, order, item, "CONSUME", line.getQuantity().negate(), before, item.getOnHandQuantity());
        }
        reservation.setStatus("CONSUMED");
        return true;
    }

    public boolean release(EntityManager em, Orders order) {
        InventoryReservation reservation = findByOrder(em, order.getOrderId());
        if (reservation == null || !canTransition(reservation.getStatus(), "RELEASED")) return false;
        for (InventoryReservationItem line : reservation.getItems().stream().sorted((a, b) -> Integer.compare(a.getInventoryItem().getInventoryItemId(), b.getInventoryItem().getInventoryItemId())).toList()) {
            InventoryItem item = items.lock(em, line.getInventoryItem().getInventoryItemId());
            BigDecimal before = item.getReservedQuantity();
            item.release(line.getQuantity());
            record(em, order, item, "RELEASE", line.getQuantity().negate(), before, item.getReservedQuantity());
        }
        reservation.setStatus("RELEASED");
        return true;
    }

    public boolean transition(EntityManager em, Orders order, String toStatus) {
        return "CONSUMED".equals(toStatus) ? consume(em, order) : "RELEASED".equals(toStatus) && release(em, order);
    }

    static String cancellationTransactionType(String status) {
        return "RESERVED".equals(status) ? "RELEASE" : "CONSUMED".equals(status) ? "WASTE" : null;
    }

    public boolean cancel(EntityManager em, Orders order) {
        InventoryReservation reservation = findByOrder(em, order.getOrderId());
        if (reservation == null) return hasLegacyReservationEvidence(em, order.getOrderId());
        if (reservation.getItems() == null || reservation.getItems().isEmpty()) return false;
        if ("RESERVED".equals(reservation.getStatus())) return release(em, order);
        if (!canTransition(reservation.getStatus(), "WASTED")) return false;
        for (InventoryReservationItem line : reservation.getItems().stream().sorted((a, b) -> Integer.compare(a.getInventoryItem().getInventoryItemId(), b.getInventoryItem().getInventoryItemId())).toList()) {
            InventoryItem item = items.lock(em, line.getInventoryItem().getInventoryItemId());
            BigDecimal onHand = item.getOnHandQuantity();
            record(em, order, item, "WASTE", line.getQuantity().negate(), onHand, onHand);
        }
        reservation.setStatus("WASTED");
        return true;
    }

    public boolean hasReservations(EntityManager em, int orderId) { return findByOrder(em, orderId) != null; }

    private boolean hasLegacyReservationEvidence(EntityManager em, int orderId) {
        Number count = (Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM InventoryReservationLegacyHistory WHERE order_id = :orderId")
                .setParameter("orderId", orderId).getSingleResult();
        return count != null && count.longValue() > 0;
    }

    private boolean equivalent(InventoryReservation reservation, Map<Integer, BigDecimal> demand) {
        if (!"RESERVED".equals(reservation.getStatus()) || reservation.getItems() == null || reservation.getItems().size() != demand.size()) return false;
        for (InventoryReservationItem line : reservation.getItems()) {
            BigDecimal expected = demand.get(line.getInventoryItem().getInventoryItemId());
            if (expected == null || expected.compareTo(line.getQuantity()) != 0) return false;
        }
        return true;
    }

    private InventoryReservation findByOrder(EntityManager em, int orderId) {
        List<InventoryReservation> reservations = em.createQuery("SELECT DISTINCT r FROM InventoryReservation r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem WHERE r.order.orderId = :orderId", InventoryReservation.class)
                .setParameter("orderId", orderId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
        return reservations.isEmpty() ? null : reservations.get(0);
    }

    private void record(EntityManager em, Orders order, InventoryItem item, String type, BigDecimal quantity, BigDecimal before, BigDecimal after) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setOrder(order);
        transaction.setInventoryItem(item);
        transaction.setTransactionType(type);
        transaction.setQuantity(scale(quantity));
        transaction.setQuantityBefore(scale(before));
        transaction.setQuantityAfter(scale(after));
        if (Set.of("CONSUME", "WASTE").contains(type)) {
            BigDecimal cost=item.getAverageUnitCost().setScale(4,RoundingMode.HALF_UP);
            transaction.setUnitCostSnapshot(cost);
            transaction.setTotalCost(quantity.abs().multiply(cost).setScale(4,RoundingMode.HALF_UP));
        }
        em.persist(transaction);
    }

    private BigDecimal scale(BigDecimal quantity) { return quantity.setScale(4, RoundingMode.HALF_UP); }
}
