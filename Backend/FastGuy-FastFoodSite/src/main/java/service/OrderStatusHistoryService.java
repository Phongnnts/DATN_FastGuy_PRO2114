package service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.OrderStatusHistoryDAO;
import entity.OrderStatusHistory;
import jakarta.persistence.EntityManager;

public class OrderStatusHistoryService {
    private OrderStatusHistoryDAO orderStatusHistoryDAO = new OrderStatusHistoryDAO();

    public void record(int orderId, Integer actorUserId, String actorRole, String fromStatus, String toStatus, String note) {
        try {
            orderStatusHistoryDAO.save(newHistory(orderId, actorUserId, actorRole, fromStatus, toStatus, note));
        } catch (RuntimeException e) {
            // An audit failure must not report a committed order/status update as failed.
            System.err.println("Unable to record order history: " + e.getMessage());
        }
    }

    public void record(EntityManager em, int orderId, Integer actorUserId, String actorRole, String fromStatus, String toStatus, String note) {
        em.persist(newHistory(orderId, actorUserId, actorRole, fromStatus, toStatus, note));
    }

    private OrderStatusHistory newHistory(int orderId, Integer actorUserId, String actorRole, String fromStatus, String toStatus, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setActorUserId(actorUserId);
        history.setActorRole(actorRole);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setNote(note);
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }

    public List<Map<String, Object>> getByOrderId(int orderId) {
        return orderStatusHistoryDAO.findByOrderId(orderId).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(OrderStatusHistory h) {
        Map<String, Object> m = new HashMap<>();
        m.put("historyId", h.getHistoryId());
        m.put("orderId", h.getOrderId());
        m.put("actorUserId", h.getActorUserId());
        m.put("actorRole", h.getActorRole());
        m.put("fromStatus", h.getFromStatus());
        m.put("toStatus", h.getToStatus());
        m.put("status", h.getToStatus());
        m.put("note", h.getNote() != null ? h.getNote() : "");
        m.put("time", h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
        return m;
    }
}
