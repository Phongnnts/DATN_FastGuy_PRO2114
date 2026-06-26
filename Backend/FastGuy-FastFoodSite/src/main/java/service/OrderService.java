package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import dao.CartDAO;
import dao.DeliveryZoneDAO;
import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.UserDAO;
import entity.Cart;
import entity.CartItem;
import entity.DeliveryZone;
import entity.OrderItem;
import entity.Orders;
import entity.User;

public class OrderService {
    private CartDAO cartDAO = new CartDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();
    private UserDAO userDAO = new UserDAO();

    public Orders checkout(int userId, int zoneId, String address, String phone,
                           String deliveryNote, String paymentMethod) {
        User user = userDAO.findById(userId);
        if (user == null) return null;

        Cart cart = cartDAO.findByUserId(userId);
        if (cart == null) return null;

        List<CartItem> cartItems = cartDAO.getItems(cart.getCartId());
        if (cartItems.isEmpty()) return null;

        DeliveryZone zone = zoneId > 0 ? deliveryZoneDAO.findById(zoneId) : null;
        BigDecimal shippingFee = zone != null ? zone.getShippingFee() : BigDecimal.ZERO;

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            totalAmount = totalAmount.add(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        BigDecimal finalAmount = totalAmount.add(shippingFee);

        Orders order = new Orders();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setCustomerName(user.getFullName());
        order.setCustomerPhone(phone != null ? phone : user.getPhone());
        order.setCustomerAddress(address);
        order.setZone(zone);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(finalAmount);
        order.setShippingProvider(zone != null ? "FALLBACK_ZONE" : "NONE");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setDeliveryNote(deliveryNote);
        order.setCreatedAt(LocalDateTime.now());

        ordersDAO.save(order);

        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setVariant(ci.getVariant());
            oi.setProductName(ci.getProduct().getName());
            oi.setVariantName(ci.getVariant() != null ? ci.getVariant().getVariantName() : "");
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setTotalPrice(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            orderItemDAO.save(oi);
        }

        cartDAO.clearCart(cart.getCartId());

        return order;
    }
}
