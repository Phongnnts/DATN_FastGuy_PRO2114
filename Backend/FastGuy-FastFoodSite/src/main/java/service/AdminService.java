package service;

import dto.OrderDTO;
import entity.Orders;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminService {
    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final ProductRepository productRepository = new ProductRepository();

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalRevenue", orderRepository.sumRevenue());
        stats.put("totalProducts", productRepository.countAvailable());

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (String status : List.of("PENDING", "CONFIRMED", "READY", "DELIVERING", "DELIVERED", "CANCELLED", "FAILED")) {
            ordersByStatus.put(status, orderRepository.countByStatus(status));
        }
        stats.put("ordersByStatus", ordersByStatus);

        List<Orders> recentOrders = orderRepository.findRecent(5);
        stats.put("recentOrders", recentOrders.stream().map(this::toDTO).collect(Collectors.toList()));

        return stats;
    }

    private OrderDTO toDTO(Orders o) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(o.getOrderId());
        dto.setOrderCode(o.getOrderCode());
        dto.setCustomerName(o.getCustomerName());
        dto.setCustomerPhone(o.getCustomerPhone());
        dto.setCustomerAddress(o.getCustomerAddress());
        if (o.getZone() != null) {
            dto.setZoneId(o.getZone().getZoneId());
            dto.setZoneName(o.getZone().getDistrictName());
        }
        dto.setTotalAmount(o.getTotalAmount());
        dto.setShippingFee(o.getShippingFee());
        dto.setFinalAmount(o.getFinalAmount());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setOrderStatus(o.getOrderStatus());
        if (o.getStaff() != null) dto.setStaffId(o.getStaff().getUserId());
        if (o.getShipper() != null) dto.setShipperId(o.getShipper().getUserId());
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }
}
