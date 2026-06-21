package service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import dao.IngredientDAO;
import dao.OrdersDAO;
import dao.ScheduleDAO;
import entity.Schedule;

public class StaffService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ScheduleDAO scheduleDAO = new ScheduleDAO();
    private IngredientDAO ingredientDAO = new IngredientDAO();

    public Map<String, Object> getDashboard(int userId) {
        long pendingOrders = ordersDAO.countByStatus("PENDING");
        long confirmedOrders = ordersDAO.countByStatus("CONFIRMED");

        Schedule todaySchedule = scheduleDAO.findByUserAndDate(userId, LocalDate.now());

        long lowStockCount = ingredientDAO.countLowStock();

        Map<String, Object> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", ordersDAO.countByStatus("PENDING"));
        ordersByStatus.put("CONFIRMED", ordersDAO.countByStatus("CONFIRMED"));
        ordersByStatus.put("PREPARING", ordersDAO.countByStatus("PREPARING"));
        ordersByStatus.put("READY", ordersDAO.countByStatus("READY"));
        ordersByStatus.put("DELIVERING", ordersDAO.countByStatus("DELIVERING"));
        ordersByStatus.put("DELIVERED", ordersDAO.countByStatus("DELIVERED"));
        ordersByStatus.put("CANCELLED", ordersDAO.countByStatus("CANCELLED"));

        long ordersToday = ordersDAO.countToday();

        Map<String, Object> data = new HashMap<>();
        data.put("pendingOrders", pendingOrders);
        data.put("confirmedOrders", confirmedOrders);
        data.put("todaySchedule", todaySchedule != null ? Map.of(
                "workDate", todaySchedule.getWorkDate().toString(),
                "shift", todaySchedule.getShift()
        ) : null);
        data.put("lowStockIngredients", lowStockCount);
        data.put("ordersByStatus", ordersByStatus);
        data.put("ordersToday", ordersToday);

        return data;
    }
}
