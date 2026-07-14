package service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dao.SupportTicketDAO;
import entity.Orders;
import entity.SupportTicket;
import entity.User;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class SupportTicketService {
    private static final Set<String> CATEGORIES = Set.of("MISSING_ITEM", "COLD_FOOD", "WRONG_ITEM", "LATE_DELIVERY", "OTHER");
    private static final Set<String> STATUSES = Set.of("OPEN", "PROCESSING", "RESOLVED");
    private SupportTicketDAO ticketDAO = new SupportTicketDAO();
    private NotificationService notificationService = new NotificationService();

    public List<Map<String, Object>> getForUser(int userId) {
        return ticketDAO.findByUserId(userId).stream().map(this::toMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getForStaff(boolean openOnly) {
        return ticketDAO.findAll(openOnly).stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> create(int userId, Integer orderId, String subject, String category, String description) {
        subject = text(subject);
        description = text(description);
        if (subject == null || subject.length() > 255 || description == null || description.length() > 2000 || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Invalid ticket data");
        }
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = null;
            if (orderId != null) {
                order = em.find(Orders.class, orderId);
                if (order == null || order.getUser() == null || order.getUser().getUserId() != userId) {
                    throw new IllegalArgumentException("Order not found");
                }
            }
            SupportTicket ticket = new SupportTicket();
            ticket.setUser(em.getReference(User.class, userId));
            ticket.setOrder(order);
            ticket.setSubject(subject);
            ticket.setCategory(category);
            ticket.setDescription(description);
            ticket.setStatus("OPEN");
            ticket.setCreatedAt(LocalDateTime.now());
            em.persist(ticket);
            em.getTransaction().commit();
            return toMap(ticket);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> update(int ticketId, int staffId, String status, String resolution) {
        if (!STATUSES.contains(status)) throw new IllegalArgumentException("Invalid status");
        resolution = text(resolution);
        if (resolution != null && resolution.length() > 2000) throw new IllegalArgumentException("Invalid resolution");
        if ("RESOLVED".equals(status) && resolution == null) throw new IllegalArgumentException("Resolution is required");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            SupportTicket ticket = em.find(SupportTicket.class, ticketId);
            if (ticket == null) throw new IllegalArgumentException("Ticket not found");
            boolean changed = !status.equals(ticket.getStatus()) || !same(resolution, ticket.getResolution());
            ticket.setStatus(status);
            ticket.setResolution(resolution);
            ticket.setStaff(em.getReference(User.class, staffId));
            ticket.setResolvedAt("RESOLVED".equals(status) ? LocalDateTime.now() : null);
            em.getTransaction().commit();
            Map<String, Object> result = toMap(ticket);
            if (changed) notificationService.notifyUser(ticket.getUser().getUserId(), "Cập nhật hỗ trợ", "Yêu cầu #" + ticketId + " đã chuyển sang " + status, "SUPPORT_TICKET", "/account/support");
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private Map<String, Object> toMap(SupportTicket ticket) {
        Map<String, Object> item = new HashMap<>();
        item.put("ticketId", ticket.getTicketId());
        item.put("userId", ticket.getUser() != null ? ticket.getUser().getUserId() : null);
        item.put("customerName", ticket.getUser() != null ? ticket.getUser().getFullName() : null);
        item.put("orderId", ticket.getOrder() != null ? ticket.getOrder().getOrderId() : null);
        item.put("orderCode", ticket.getOrder() != null ? ticket.getOrder().getOrderCode() : null);
        item.put("subject", ticket.getSubject());
        item.put("category", ticket.getCategory());
        item.put("description", ticket.getDescription());
        item.put("status", ticket.getStatus());
        item.put("staffId", ticket.getStaff() != null ? ticket.getStaff().getUserId() : null);
        item.put("staffName", ticket.getStaff() != null ? ticket.getStaff().getFullName() : null);
        item.put("resolution", ticket.getResolution());
        item.put("createdAt", ticket.getCreatedAt());
        item.put("resolvedAt", ticket.getResolvedAt());
        return item;
    }

    private String text(String value) {
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
