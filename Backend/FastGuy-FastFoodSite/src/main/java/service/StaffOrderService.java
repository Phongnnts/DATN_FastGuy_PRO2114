package service;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import entity.Orders;

import java.time.LocalDateTime;
import java.util.List;

public class StaffOrderService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();

    public List<Orders> getPendingOrders() {
        return ordersDAO.findByStatus("PENDING");
    }

    public List<Orders> getConfirmedOrders() {
        return ordersDAO.findByStatus("CONFIRMED");
    }

    public Orders getOrderDetail(int orderId) {
        return ordersDAO.findById(orderId);
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null) return false;

        order.setOrderStatus(status);
        order.setStaff(new entity.User() {{ setUserId(staffId); }});
        if ("CONFIRMED".equals(status)) {
            order.setConfirmedAt(LocalDateTime.now());
        } else if ("CANCELLED".equals(status)) {
            order.setCancelledAt(LocalDateTime.now());
        }
        ordersDAO.save(order);
        return true;
    }
}
