package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.OrdersDAO;
import entity.Orders;
import entity.WorkShift;

public class StaffService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private WorkShiftService workShiftService = new WorkShiftService();

    public Map<String, Object> getDashboard(int staffId) {
        LocalDateTime now = LocalDateTime.now(WorkShiftService.BUSINESS_ZONE);
        WorkShift shift = workShiftService.currentCheckedInShift(staffId);
        if (shift == null) throw new IllegalArgumentException("Checked-in shift required");

        Map<String, Object> ordersByStatus = new HashMap<>();
        for (String status : List.of("PENDING", "CONFIRMED", "PREPARING", "READY", "ASSIGNED", "PICKED_UP", "DELIVERED", "CANCELLED")) {
            ordersByStatus.put(status, ordersDAO.countByStatus(status));
        }

        Object[] shiftSummary = ordersDAO.summarizeShift(shift.getCheckInAt(), now);
        Map<String, Object> currentShift = new HashMap<>();
        currentShift.put("shiftId", shift.getShiftId());
        currentShift.put("startTime", shift.getStartTime());
        currentShift.put("endTime", shift.getEndTime());
        currentShift.put("checkInAt", shift.getCheckInAt());
        currentShift.put("status", shift.getStatus());

        List<Map<String, Object>> priorityOrders = new ArrayList<>();
        for (Orders order : ordersDAO.findPriorityOrders()) {
            Map<String, Object> item = new HashMap<>();
            item.put("orderId", order.getOrderId());
            item.put("orderCode", order.getOrderCode());
            item.put("customerName", order.getCustomerName());
            item.put("status", order.getOrderStatus());
            item.put("createdAt", order.getCreatedAt());
            item.put("finalAmount", order.getFinalAmount());
            priorityOrders.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pendingOrders", ordersByStatus.get("PENDING"));
        data.put("confirmedOrders", ordersByStatus.get("CONFIRMED"));
        data.put("ordersByStatus", ordersByStatus);
        data.put("ordersToday", ordersDAO.countToday());
        data.put("overdueOrders", ordersDAO.countOverdueActive(now.minusMinutes(25)));
        data.put("awaitingShipperOrders", ordersByStatus.get("READY"));
        data.put("currentShift", currentShift);
        data.put("shiftCompletedOrders", number(shiftSummary[0]).longValue());
        data.put("shiftFailedOrders", number(shiftSummary[1]).longValue());
        data.put("shiftNetRevenue", decimal(shiftSummary[2]));
        data.put("priorityOrders", priorityOrders);
        data.put("updatedAt", now);
        return data;
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0L;
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal amount ? amount : BigDecimal.ZERO;
    }
}
