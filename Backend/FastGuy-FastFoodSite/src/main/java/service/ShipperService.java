package service;

import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class ShipperService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private UserDAO userDAO = new UserDAO();

    public List<Orders> getAvailableOrders() {
        return ordersDAO.findByStatusAndNoShipper("READY");
    }

    public List<Orders> getMyOrders(int shipperId) {
        return ordersDAO.findByShipperId(shipperId);
    }

    public List<Orders> getMyActiveOrders(int shipperId) {
        return ordersDAO.findByShipperIdAndStatus(shipperId, "PICKED_UP");
    }

    public List<Orders> getMyHistory(int shipperId) {
        return ordersDAO.findByShipperIdAndStatus(shipperId, "DELIVERED");
    }

    public boolean pickUpOrder(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null || !"READY".equals(order.getOrderStatus())) return false;
        if (order.getShipper() != null) return false;
        User shipper = userDAO.findById(shipperId);
        if (shipper == null) return false;
        order.setOrderStatus("PICKED_UP");
        order.setShipper(shipper);
        order.setPickedUpAt(LocalDateTime.now());
        ordersDAO.save(order);
        return true;
    }

    public boolean deliverOrder(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null || !"PICKED_UP".equals(order.getOrderStatus())) return false;
        if (order.getShipper() == null || order.getShipper().getUserId() != shipperId) return false;
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now());
        ordersDAO.save(order);
        return true;
    }

    public boolean cancelOrder(int orderId, int shipperId, String reason) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null) return false;
        if (order.getShipper() == null || order.getShipper().getUserId() != shipperId) return false;
        if (!"PICKED_UP".equals(order.getOrderStatus())) return false;
        order.setOrderStatus("CANCELLED");
        order.setFailureReason(reason);
        ordersDAO.save(order);
        return true;
    }
}
