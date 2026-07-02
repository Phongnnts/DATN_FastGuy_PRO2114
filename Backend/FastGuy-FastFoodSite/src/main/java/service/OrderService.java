package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dao.CartDAO;
import dao.DeliveryZoneDAO;
import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.ProductDAO;
import dao.UserDAO;
import entity.Cart;
import entity.CartItem;
import entity.DeliveryZone;
import entity.OrderItem;
import entity.Orders;
import entity.Product;
import entity.ProductVariant;
import entity.User;

public class OrderService {
    private CartDAO cartDAO = new CartDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private UserDAO userDAO = new UserDAO();
    private ProductDAO productDAO = new ProductDAO();
    private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();
    private CouponService couponService = new CouponService();

    public Orders checkout(int userId, int zoneId, String address, String phone,
                           String deliveryNote, String paymentMethod,
                           Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode,
                           String toProvinceName, String toDistrictName, String toWardName,
                           BigDecimal shippingFee, String couponCode) {
        User user = userDAO.findById(userId);
        if (user == null) return null;

        Cart cart = cartDAO.findByUserId(userId);
        if (cart == null) return null;

        List<CartItem> cartItems = cartDAO.getItems(cart.getCartId());
        if (cartItems.isEmpty()) return null;

        if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) < 0) {
            shippingFee = BigDecimal.ZERO;
        }

        DeliveryZone zone = zoneId > 0 ? deliveryZoneDAO.findById(zoneId) : null;
        if (zone != null && zone.getShippingFee() != null
                && shippingFee.compareTo(zone.getShippingFee()) != 0) {
            throw new RuntimeException("Shipping fee mismatch");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            totalAmount = totalAmount.add(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        int couponId = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Map<String, Object> vr = couponService.verify(couponCode.trim(), totalAmount, shippingFee, userId);
            if (Boolean.TRUE.equals(vr.get("valid"))) {
                discountAmount = (BigDecimal) vr.get("discount");
                couponId = (int) vr.get("couponId");
            }
        }
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        Orders order = new Orders();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setCustomerName(user.getFullName());
        order.setCustomerPhone(phone != null ? phone : user.getPhone());
        order.setCustomerAddress(address);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(finalAmount);
        order.setGhnProvinceId(ghnProvinceId);
        order.setGhnDistrictId(ghnDistrictId);
        order.setGhnWardCode(ghnWardCode);
        order.setToProvinceName(toProvinceName);
        order.setToDistrictName(toDistrictName);
        order.setToWardName(toWardName);
        order.setShippingProvider("GHN");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode != null && !couponCode.trim().isEmpty() ? couponCode.trim().toUpperCase() : null);
        order.setDeliveryNote(deliveryNote);
        order.setCreatedAt(LocalDateTime.now());

        order.setZone(zone);
        if (order.getShippingProvider() == null || "GHN".equals(order.getShippingProvider())) {
            order.setShippingProvider(zone != null ? "FALLBACK_ZONE" : "GHN");
        }

        ordersDAO.save(order);

        if (couponId > 0) {
            couponService.apply(couponId, order.getOrderId(), userId, discountAmount);
        }

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

    @SuppressWarnings("unchecked")
    public Orders guestCheckout(String customerName, String phone, String address,
                                String deliveryNote, String paymentMethod,
                                List<Map<String, Object>> itemsData,
                                BigDecimal shippingFee,
                                Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode,
                                String toProvinceName, String toDistrictName, String toWardName,
                                String couponCode) {
        if (customerName == null || phone == null || address == null || itemsData == null || itemsData.isEmpty()) {
            return null;
        }

        if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) < 0) {
            shippingFee = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> itemData : itemsData) {
            int productId = ((Number) itemData.get("productId")).intValue();
            int variantId = ((Number) itemData.get("variantId")).intValue();
            int qty = ((Number) itemData.get("quantity")).intValue();
            BigDecimal unitPrice = itemData.get("unitPrice") instanceof Number
                    ? BigDecimal.valueOf(((Number) itemData.get("unitPrice")).doubleValue())
                    : new BigDecimal(itemData.get("unitPrice").toString());

            Product product = productDAO.findById(productId);
            if (product == null) return null;
            ProductVariant variant = productDAO.findVariantById(variantId);
            if (variant == null) return null;

            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        int couponId = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Map<String, Object> vr = couponService.verify(couponCode.trim(), totalAmount, shippingFee, null);
            if (Boolean.TRUE.equals(vr.get("valid"))) {
                discountAmount = (BigDecimal) vr.get("discount");
                couponId = (int) vr.get("couponId");
            }
        }
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        Orders order = new Orders();
        order.setOrderCode("GST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(null);
        order.setCustomerName(customerName);
        order.setCustomerPhone(phone);
        order.setCustomerAddress(address);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(finalAmount);
        order.setGhnProvinceId(ghnProvinceId);
        order.setGhnDistrictId(ghnDistrictId);
        order.setGhnWardCode(ghnWardCode);
        order.setToProvinceName(toProvinceName);
        order.setToDistrictName(toDistrictName);
        order.setToWardName(toWardName);
        order.setShippingProvider("GHN");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode != null && !couponCode.trim().isEmpty() ? couponCode.trim().toUpperCase() : null);
        order.setDeliveryNote(deliveryNote);
        order.setCreatedAt(LocalDateTime.now());
        ordersDAO.save(order);

        if (couponId > 0) {
            couponService.apply(couponId, order.getOrderId(), null, discountAmount);
        }

        for (Map<String, Object> itemData : itemsData) {
            int productId = ((Number) itemData.get("productId")).intValue();
            int variantId = ((Number) itemData.get("variantId")).intValue();
            int qty = ((Number) itemData.get("quantity")).intValue();
            BigDecimal unitPrice = itemData.get("unitPrice") instanceof Number
                    ? BigDecimal.valueOf(((Number) itemData.get("unitPrice")).doubleValue())
                    : new BigDecimal(itemData.get("unitPrice").toString());

            Product product = productDAO.findById(productId);
            ProductVariant variant = productDAO.findVariantById(variantId);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setVariant(variant);
            oi.setProductName(product.getName());
            oi.setVariantName(variant != null ? variant.getVariantName() : "");
            oi.setQuantity(qty);
            oi.setUnitPrice(unitPrice);
            oi.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)));
            orderItemDAO.save(oi);
        }

        return order;
    }
}
