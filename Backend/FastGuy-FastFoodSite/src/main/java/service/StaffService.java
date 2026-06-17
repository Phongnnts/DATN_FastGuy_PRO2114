package service;

import dao.IngredientDAO;
import dao.OrdersDAO;
import dao.ScheduleDAO;
import dao.UserDAO;
import entity.Schedule;
import utils.JwtUtil;
import utils.JsonUtil;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class StaffService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ScheduleDAO scheduleDAO = new ScheduleDAO();
    private IngredientDAO ingredientDAO = new IngredientDAO();

    public Map<String, Object> getDashboard(int userId) {
        long pendingOrders = ordersDAO.countByStatus("PENDING");
        long confirmedOrders = ordersDAO.countByStatus("CONFIRMED");

        Schedule todaySchedule = scheduleDAO.findByUserAndDate(userId, LocalDate.now());

        long lowStockCount = ingredientDAO.countLowStock();

        Map<String, Object> data = new HashMap<>();
        data.put("pendingOrders", pendingOrders);
        data.put("confirmedOrders", confirmedOrders);
        data.put("todaySchedule", todaySchedule != null ? Map.of(
                "workDate", todaySchedule.getWorkDate().toString(),
                "shift", todaySchedule.getShift()
        ) : null);
        data.put("lowStockIngredients", lowStockCount);

        return data;
    }
}
