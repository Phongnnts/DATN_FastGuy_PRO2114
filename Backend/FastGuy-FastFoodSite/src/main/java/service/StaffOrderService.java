package service;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class StaffOrderService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private UserDAO userDAO = new UserDAO();
    private OrderTransitionService transitionService = new OrderTransitionService();
    private StoreConfigService storeConfigService = new StoreConfigService();
    private DispatchOrderPolicy dispatchPolicy = new DispatchOrderPolicy();
    private Supplier<LocalDateTime> businessNow = WorkShiftService::businessNow;

    StaffOrderService(OrdersDAO ordersDAO, StoreConfigService storeConfigService,
                      DispatchOrderPolicy dispatchPolicy, Supplier<LocalDateTime> businessNow) {
        this.ordersDAO = ordersDAO;
        this.storeConfigService = storeConfigService;
        this.dispatchPolicy = dispatchPolicy;
        this.businessNow = businessNow;
    }

    public StaffOrderService() {}

    public List<Orders> getPendingOrders() {
        return ordersDAO.findByStatus("PENDING");
    }

    public List<Orders> getConfirmedOrders() {
        return ordersDAO.findByStatus("CONFIRMED");
    }

    public List<Orders> getPreparingOrders() {
        return ordersDAO.findByStatus("PREPARING");
    }

    public List<Orders> getReadyOrders() {
        return ordersDAO.findByStatus("READY");
    }

    public DispatchResult getDispatchOrders(String filter) {
        LocalDateTime now = businessNow.get();
        Map<String, String> config = storeConfigService.getAll();
        String openValue = config.get(StoreConfigService.OPEN_TIME);
        String closeValue = config.get(StoreConfigService.CLOSE_TIME);
        if (openValue == null || openValue.isBlank() || closeValue == null || closeValue.isBlank())
            throw new IllegalArgumentException("Missing business hours config");
        LocalTime open;
        LocalTime close;
        try {
            open = LocalTime.parse(openValue);
            close = LocalTime.parse(closeValue);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid business hours config", e);
        }
        List<DispatchItem> priority = new ArrayList<>();
        List<DispatchItem> recent = new ArrayList<>();
        List<DispatchItem> review = new ArrayList<>();

        for (Orders order : ordersDAO.findDispatchCandidates()) {
            String classification = dispatchPolicy.classify(order, now, open, close);
            if ("REVIEW".equals(classification)) {
                review.add(new DispatchItem(order, classification, null));
            } else if (classification != null) {
                Long minutes = dispatchPolicy.minutesUntilClose(order, now, open, close);
                if (dispatchPolicy.isPriority(order, now, dispatchPolicy.closingAt(order.getCreatedAt(), open, close)))
                    priority.add(new DispatchItem(order, "PRIORITY", minutes));
                if (dispatchPolicy.isNew(order, now)) recent.add(new DispatchItem(order, "NEW", minutes));
            }
        }

        priority.sort(Comparator.comparing(DispatchItem::minutesUntilClose, Comparator.nullsLast(Long::compareTo))
                .thenComparing(item -> item.order().getReadyAt(), Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt(item -> item.order().getOrderId()));
        recent.sort(Comparator.comparing((DispatchItem item) -> item.order().getReadyAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((DispatchItem item) -> item.order().getOrderId(), Comparator.reverseOrder()));
        review.sort(Comparator.comparing((DispatchItem item) -> item.order().getDeliveryFailedAt(), Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt(item -> item.order().getOrderId()));

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("priority", (long) priority.size());
        counts.put("new", (long) recent.size());
        counts.put("review", (long) review.size());
        List<DispatchItem> items = switch (filter) {
            case "PRIORITY" -> priority;
            case "NEW" -> recent;
            case "REVIEW" -> review;
            default -> throw new IllegalArgumentException("Invalid dispatch filter");
        };
        return new DispatchResult(items, counts, now, open, close);
    }

    public record DispatchItem(Orders order, String classification, Long minutesUntilClose) {}
    public record DispatchResult(List<DispatchItem> items, Map<String, Long> counts,
                                 LocalDateTime serverTime, LocalTime openTime, LocalTime closeTime) {}

    public Orders getOrderDetail(int orderId) {
        return ordersDAO.findById(orderId);
    }

    public List<WorkShift> getAvailableShipperShifts() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime now = WorkShiftService.businessNow();
            return em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.role = 'SHIPPER' AND ws.user.status = 'ACTIVE' " +
                            "AND ws.shiftDate = :today AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL " +
                            "AND ws.checkOutAt IS NULL ORDER BY ws.checkInAt DESC, ws.shiftId DESC", WorkShift.class)
                    .setParameter("today", now.toLocalDate())
                    .getResultList().stream()
                    .filter(shift -> WorkShiftService.isValidCheckedInShift(shift, now))
                    .collect(java.util.stream.Collectors.toMap(shift -> shift.getUser().getUserId(), shift -> shift, (first, ignored) -> first, java.util.LinkedHashMap::new))
                    .values().stream().toList();
        } finally {
            em.close();
        }
    }

    public long countActiveOrders(int shipperId, LocalDateTime shiftStart) {
        return ordersDAO.countActiveByShipper(shipperId, shiftStart);
    }

    public OrderTransitionService.MutationResult assignShipper(int orderId, int shipperId, int staffId, String expectedStatus) {
        Orders order = getOrderDetail(orderId);
        OrderTransitionService.MutationResult result = transitionService.transition(orderId, "ASSIGNED", "STAFF", staffId, "Gán shipper", shipperId, null, expectedStatus);
        return result;
    }

    public OrderTransitionService.MutationResult updateStatus(int orderId, String status, int staffId, String failureReason, String expectedStatus) {
        Orders order = getOrderDetail(orderId);
        OrderTransitionService.MutationResult result = transitionService.transition(orderId, status, "STAFF", staffId, failureReason, null, null, expectedStatus);
        if (result == OrderTransitionService.MutationResult.SUCCESS && order != null && order.getUser() != null) {
        }
        return result;
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        return transitionService.transition(orderId, status, "STAFF", staffId, null, null, null);
    }

    public List<Orders> getDeliveryFailureQueue() {
        return ordersDAO.findDeliveryFailureQueue();
    }

    public OrderTransitionService.MutationResult retryDelivery(int orderId, int staffId, String expectedStatus, int shipperId, String retryMode, LocalDateTime scheduledAt, String note) {
        return transitionService.retryDelivery(orderId, staffId, expectedStatus, shipperId, retryMode, scheduledAt, note);
    }

    public OrderTransitionService.MutationResult startScheduledRetry(int orderId, int staffId, String expectedStatus) {
        return transitionService.startScheduledRetry(orderId, staffId, expectedStatus);
    }

    public OrderTransitionService.MutationResult returnToStore(int orderId, int staffId, String expectedStatus, String note) {
        return transitionService.returnToStore(orderId, staffId, expectedStatus, note);
    }
}
