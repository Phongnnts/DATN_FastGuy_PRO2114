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
    private ShippingService shippingService = new ShippingService();

    public Orders checkout(int userId, int zoneId, String address, String phone,
                           String deliveryNote, String paymentMethod,
                           Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode,
                           String toProvinceName, String toDistrictName, String toWardName,
                           BigDecimal shippingFee, String couponCode) {
        User user = userDAO.findById(userId);
        if (user == null) return null;
        if (!isValidAddress(address) || !isValidPhone(phone != null ? phone : user.getPhone())) return null;

        Cart cart = cartDAO.findByUserId(userId);
        if (cart == null) return null;

        List<CartItem> cartItems = cartDAO.getItems(cart.getCartId());
        if (cartItems.isEmpty()) return null;

        shippingFee = normalizeShippingFee(shippingFee, ghnDistrictId, ghnWardCode);

        DeliveryZone zone = zoneId > 0 ? deliveryZoneDAO.findById(zoneId) : null;
        if (zone != null && zone.getShippingFee() != null
                && shippingFee.compareTo(zone.getShippingFee()) != 0) {
            throw new RuntimeException("Shipping fee mismatch");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            if (ci.getQuantity() <= 0) return null;
            Product product = ci.getProduct();
            ProductVariant variant = ci.getVariant();
            if (product == null || !"AVAILABLE".equals(product.getStatus())) return null;
            if (variant == null || variant.getProduct() == null || variant.getProduct().getProductId() != product.getProductId()) return null;
            if (!"AVAILABLE".equals(variant.getStatus())) return null;
            if (variant.getQuantityAvailable() != null && variant.getQuantityAvailable() < ci.getQuantity()) return null;
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getBasePrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) return null;
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        int couponId = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Map<String, Object> vr = couponService.verify(couponCode.trim(), totalAmount, shippingFee, userId);
            if (Boolean.TRUE.equals(vr.get("valid"))) {
                discountAmount = (BigDecimal) vr.get("discount");
                couponId = (int) vr.get("couponId");
            } else {
                throw new IllegalArgumentException(String.valueOf(vr.get("message")));
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
            BigDecimal unitPrice = ci.getVariant().getPrice() != null ? ci.getVariant().getPrice() : ci.getProduct().getBasePrice();
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setVariant(ci.getVariant());
            oi.setProductName(ci.getProduct().getName());
            oi.setVariantName(ci.getVariant() != null ? ci.getVariant().getVariantName() : "");
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(unitPrice);
            oi.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
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
        if (customerName == null || customerName.trim().length() < 2 || phone == null || address == null || itemsData == null || itemsData.isEmpty()) {
            return null;
        }
        if (!isValidPhone(phone) || !isValidAddress(address)) return null;

        shippingFee = normalizeShippingFee(shippingFee, ghnDistrictId, ghnWardCode);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> itemData : itemsData) {
            int productId = ((Number) itemData.get("productId")).intValue();
            int variantId = ((Number) itemData.get("variantId")).intValue();
            int qty = ((Number) itemData.get("quantity")).intValue();
            if (qty <= 0) return null;

            Product product = productDAO.findById(productId);
            if (product == null || !"AVAILABLE".equals(product.getStatus())) return null;
            ProductVariant variant = productDAO.findVariantById(variantId);
            if (variant == null || variant.getProduct() == null || variant.getProduct().getProductId() != productId) return null;
            if (!"AVAILABLE".equals(variant.getStatus())) return null;
            if (variant.getQuantityAvailable() != null && variant.getQuantityAvailable() < qty) return null;

            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getBasePrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) return null;
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        int couponId = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Map<String, Object> vr = couponService.verify(couponCode.trim(), totalAmount, shippingFee, null);
            if (Boolean.TRUE.equals(vr.get("valid"))) {
                discountAmount = (BigDecimal) vr.get("discount");
                couponId = (int) vr.get("couponId");
            } else {
                throw new IllegalArgumentException(String.valueOf(vr.get("message")));
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
            Product product = productDAO.findById(productId);
            ProductVariant variant = productDAO.findVariantById(variantId);
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getBasePrice();

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

    private BigDecimal normalizeShippingFee(BigDecimal shippingFee, Integer ghnDistrictId, String ghnWardCode) {
        if (ghnDistrictId != null && ghnWardCode != null && !ghnWardCode.trim().isEmpty()) {
            Map<String, Object> fee = shippingService.calculateFee(ghnDistrictId, ghnWardCode, 1000, 20, 20, 10);
            Object value = fee.get("fee") != null ? fee.get("fee") : fee.get("shippingFee");
            if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
            if (value instanceof String) return new BigDecimal((String) value);
        }
        if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        return shippingFee;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.trim().matches("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    }

    private boolean isValidAddress(String address) {
        String value = address == null ? "" : address.trim();
        return value.length() >= 5 && value.length() <= 500;
    }
}
