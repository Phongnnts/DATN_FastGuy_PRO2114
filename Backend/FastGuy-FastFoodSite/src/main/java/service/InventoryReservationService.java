package service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import entity.InventoryReservation;
import entity.InventoryTransaction;
import entity.Orders;
import entity.ProductVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

public class InventoryReservationService {
    public static boolean canTransition(String from, String to) {
        return "RESERVED".equals(from) && Set.of("CONSUMED", "RELEASED").contains(to);
    }

    public void reserve(EntityManager em, Orders order, Map<Integer, Integer> quantities) {
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            ProductVariant variant = em.getReference(ProductVariant.class, entry.getKey());
            InventoryReservation reservation = new InventoryReservation();
            reservation.setOrder(order);
            reservation.setVariant(variant);
            reservation.setQuantity(entry.getValue());
            reservation.setStatus("RESERVED");
            em.persist(reservation);
            record(em, order, variant, "RESERVE", entry.getValue());
        }
    }

    public boolean transition(EntityManager em, Orders order, String toStatus) {
        List<InventoryReservation> reservations = findByOrder(em, order.getOrderId());
        if (reservations.isEmpty()) return false;
        boolean changed = false;
        for (InventoryReservation reservation : reservations) {
            if (!canTransition(reservation.getStatus(), toStatus)) continue;
            reservation.setStatus(toStatus);
            if ("RELEASED".equals(toStatus) && reservation.getVariant().getQuantityAvailable() != null) {
                ProductVariant variant = em.find(ProductVariant.class, reservation.getVariant().getVariantId(), LockModeType.PESSIMISTIC_WRITE);
                variant.setQuantityAvailable(variant.getQuantityAvailable() + reservation.getQuantity());
            }
            record(em, order, reservation.getVariant(), "RELEASED".equals(toStatus) ? "RELEASE" : "CONSUME", reservation.getQuantity());
            changed = true;
        }
        return changed;
    }

    public boolean hasReservations(EntityManager em, int orderId) {
        return !findByOrder(em, orderId).isEmpty();
    }

    private List<InventoryReservation> findByOrder(EntityManager em, int orderId) {
        return em.createQuery("SELECT r FROM InventoryReservation r WHERE r.order.orderId = :orderId ORDER BY r.variant.variantId", InventoryReservation.class)
                .setParameter("orderId", orderId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    private void record(EntityManager em, Orders order, ProductVariant variant, String type, int quantity) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setOrder(order);
        transaction.setVariant(variant);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        em.persist(transaction);
    }
}
