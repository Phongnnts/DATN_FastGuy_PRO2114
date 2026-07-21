package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import entity.Cart;
import entity.CartItem;
import entity.Coupon;
import entity.CouponRedemption;
import entity.OrderItem;
import entity.Orders;
import entity.ProductModifierOption;
import entity.Product;
import entity.ProductVariant;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class OrderService {
    private CouponService couponService = new CouponService();
    private NotificationService notificationService = new NotificationService();
    private OrderStatusHistoryService orderStatusHistoryService = new OrderStatusHistoryService();
    private StoreConfigService storeConfigService = new StoreConfigService();
    private ShippingService shippingService = new ShippingService();

    private static class CheckoutLine {
        Product product;
        ProductVariant variant;
        int quantity;
        BigDecimal unitPrice;
        List<ProductModifierOption> modifiers = new ArrayList<>();
    }

    private static class CouponResult {
        Coupon coupon;
        CouponRedemption redemption;
        BigDecimal discount = BigDecimal.ZERO;
        String code;
    }

    public Orders checkout(int userId, String customerName, String address, String phone,
                             String deliveryNote, String paymentMethod,
                            Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode,
                             String toProvinceName, String toDistrictName, String toWardName,
                             String couponCode) {
        Map<String, String> storeConfig = storeConfigService.getAll();
        BigDecimal serviceFee = validateBusinessHoursAndGetServiceFee(storeConfig);
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            User user = em.find(User.class, userId);
            if (user == null) throw new IllegalArgumentException("Không tìm thấy người dùng");
            String customerPhone = phone != null ? phone : user.getPhone();
            if (!isValidAddress(address) || !isValidPhone(customerPhone)) {
                throw new IllegalArgumentException("Thông tin giao hàng không hợp lệ");
            }

            Cart cart = findCart(em, userId);
            if (cart == null) throw new IllegalArgumentException("Giỏ hàng trống");
            List<CartItem> cartItems = findCartItems(em, cart.getCartId());
            if (cartItems.isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống");

            if (!"COD".equals(paymentMethod) && !"BANK_TRANSFER".equals(paymentMethod)) {
                throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
            }
            boolean isBankTransfer = "BANK_TRANSFER".equals(paymentMethod);
            Map<Integer, Integer> quantities = new HashMap<>();
            for (CartItem item : cartItems) {
                if (item.getVariant() == null) throw new IllegalArgumentException("Sản phẩm trong giỏ không hợp lệ");
                quantities.merge(item.getVariant().getVariantId(), item.getQuantity(), Integer::sum);
            }

            Map<Integer, CheckoutLine> lockedLines = lockAndDeduct(em, quantities, null);

            List<CheckoutLine> lines = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                CheckoutLine locked = lockedLines.get(item.getVariant().getVariantId());
                CheckoutLine line = new CheckoutLine();
                line.product = locked.product;
                line.variant = locked.variant;
                line.quantity = item.getQuantity();
                line.modifiers = modifiers(em, item.getSelectedModifierOptionIds(), locked.product.getProductId());
                line.unitPrice = locked.unitPrice.add(line.modifiers.stream().map(o -> o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
                lines.add(line);
                totalAmount = totalAmount.add(line.unitPrice.multiply(BigDecimal.valueOf(line.quantity)));
            }

            BigDecimal shippingFee = calculateGhnFee(lines, ghnDistrictId, ghnWardCode);
            CouponResult coupon = verifyCoupon(em, couponCode, totalAmount, shippingFee, userId);
            BigDecimal finalAmount = totalAmount.add(shippingFee).add(serviceFee).subtract(coupon.discount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

            String recipient = customerName != null && customerName.trim().length() >= 2 ? customerName.trim() : user.getFullName();
            Orders order = buildOrder("ORD-", user, recipient, customerPhone, address, deliveryNote, paymentMethod,
                    ghnProvinceId, ghnDistrictId, ghnWardCode, toProvinceName, toDistrictName, toWardName,
                    shippingFee, serviceFee, totalAmount, finalAmount, coupon.discount, coupon.code);
            em.persist(order);
            em.flush();
            applyCoupon(em, coupon, order.getOrderId(), userId);
            persistItems(em, order, lines);
            if (!isBankTransfer) {
                em.createQuery("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
                        .setParameter("cartId", cart.getCartId())
                        .executeUpdate();
            }

            orderStatusHistoryService.record(em, order.getOrderId(), userId, "USER", null, "PENDING", "Khách tạo đơn hàng");
            em.getTransaction().commit();
            notificationService.notifyRole("STAFF", "Đơn hàng mới", "Đơn " + order.getOrderCode() + " vừa được tạo", "ORDER_CREATED", "/staff/orders/" + order.getOrderId());
            notificationService.notifyRole("ADMIN", "Đơn hàng mới", "Đơn " + order.getOrderCode() + " vừa được tạo", "ORDER_CREATED", "/admin/orders");
            return order;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public Orders guestCheckout(String customerName, String phone, String address,
                                 String deliveryNote, String paymentMethod,
                                 List<Map<String, Object>> itemsData,
                                 Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode,
                                 String toProvinceName, String toDistrictName, String toWardName,
                                 String couponCode) {
        Map<String, String> storeConfig = storeConfigService.getAll();
        BigDecimal serviceFee = validateBusinessHoursAndGetServiceFee(storeConfig);
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            if (customerName == null || customerName.trim().length() < 2 || phone == null || address == null || itemsData == null || itemsData.isEmpty()) {
                throw new IllegalArgumentException("Thông tin đặt hàng không hợp lệ");
            }
            if (!isValidPhone(phone) || !isValidAddress(address)) throw new IllegalArgumentException("Thông tin giao hàng không hợp lệ");
            if (!"COD".equals(paymentMethod) && !"BANK_TRANSFER".equals(paymentMethod)) {
                throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
            }
            Map<Integer, Integer> quantities = new HashMap<>();
            Map<Integer, Integer> productIds = new HashMap<>();
            for (Map<String, Object> itemData : itemsData) {
                int productId = ((Number) itemData.get("productId")).intValue();
                int variantId = ((Number) itemData.get("variantId")).intValue();
                int qty = ((Number) itemData.get("quantity")).intValue();
                if (qty <= 0) throw new IllegalArgumentException("Số lượng không hợp lệ");
                quantities.merge(variantId, qty, Integer::sum);
                productIds.put(variantId, productId);
            }

            Map<Integer, CheckoutLine> lockedLines = lockAndDeduct(em, quantities, productIds);

            List<CheckoutLine> lines = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Map<String, Object> itemData : itemsData) {
                int variantId = ((Number) itemData.get("variantId")).intValue();
                int qty = ((Number) itemData.get("quantity")).intValue();
                CheckoutLine locked = lockedLines.get(variantId);
                CheckoutLine line = new CheckoutLine();
                line.product = locked.product;
                line.variant = locked.variant;
                line.quantity = qty;
                line.modifiers = modifiers(em, modifierIds(itemData), locked.product.getProductId());
                line.unitPrice = locked.unitPrice.add(line.modifiers.stream().map(o -> o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
                lines.add(line);
                totalAmount = totalAmount.add(line.unitPrice.multiply(BigDecimal.valueOf(qty)));
            }

            BigDecimal shippingFee = calculateGhnFee(lines, ghnDistrictId, ghnWardCode);
            CouponResult coupon = verifyCoupon(em, couponCode, totalAmount, shippingFee, null);
            BigDecimal finalAmount = totalAmount.add(shippingFee).add(serviceFee).subtract(coupon.discount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

            Orders order = buildOrder("GST-", null, customerName.trim(), phone.trim(), address, deliveryNote, paymentMethod,
                    ghnProvinceId, ghnDistrictId, ghnWardCode, toProvinceName, toDistrictName, toWardName,
                    shippingFee, serviceFee, totalAmount, finalAmount, coupon.discount, coupon.code);
            em.persist(order);
            em.flush();
            applyCoupon(em, coupon, order.getOrderId(), null);
            persistItems(em, order, lines);

            orderStatusHistoryService.record(em, order.getOrderId(), null, "GUEST", null, "PENDING", "Khách vãng lai tạo đơn hàng");
            em.getTransaction().commit();
            notificationService.notifyRole("STAFF", "Đơn hàng mới", "Đơn " + order.getOrderCode() + " vừa được tạo", "ORDER_CREATED", "/staff/orders/" + order.getOrderId());
            notificationService.notifyRole("ADMIN", "Đơn hàng mới", "Đơn " + order.getOrderCode() + " vừa được tạo", "ORDER_CREATED", "/admin/orders");
            return order;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean confirmStock(int orderId, int staffId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"WAITING_STOCK_CONFIRM".equals(order.getOrderStatus())) {
                em.getTransaction().rollback();
                return false;
            }

            List<OrderItem> orderItems = em.createQuery("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId", OrderItem.class)
                    .setParameter("orderId", orderId)
                    .getResultList();
            Map<Integer, Integer> quantities = new HashMap<>();
            for (OrderItem oi : orderItems) {
                if (oi.getVariant() != null) {
                    quantities.merge(oi.getVariant().getVariantId(), oi.getQuantity(), Integer::sum);
                }
            }

            Map<Integer, CheckoutLine> lockedLines = lockAndDeduct(em, quantities, null);

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem oi : orderItems) {
                CheckoutLine locked = lockedLines.get(oi.getVariant().getVariantId());
                if (locked != null) {
                    BigDecimal modifierPrice = oi.getModifiers().stream()
                            .map(m -> m.price != null ? m.price : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    oi.setUnitPrice(locked.unitPrice.add(modifierPrice));
                    oi.setTotalPrice(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
                    totalAmount = totalAmount.add(oi.getTotalPrice());
                }
            }

            BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
            BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal serviceFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
            BigDecimal finalAmount = totalAmount.add(shippingFee).add(serviceFee).subtract(discountAmount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;
            order.setTotalAmount(totalAmount);
            order.setFinalAmount(finalAmount);
            order.setOrderStatus("PENDING");
            order.setConfirmedAt(LocalDateTime.now());

            User staff = em.find(User.class, staffId);
            if (staff != null) order.setStaff(staff);

            orderStatusHistoryService.record(em, orderId, staffId, "STAFF", "WAITING_STOCK_CONFIRM", "PENDING", "Xác nhận còn hàng");
            em.getTransaction().commit();
            if (order.getUser() != null) {
                notificationService.notifyUser(order.getUser().getUserId(),
                        "Đơn hàng đã được xác nhận",
                        "Đơn " + order.getOrderCode() + " đã được xác nhận tồn kho, vui lòng thanh toán",
                        "ORDER_STATUS",
                        "/account/orders/" + orderId);
            }
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean revertStockConfirmation(int orderId, int staffId, String reason) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"PENDING".equals(order.getOrderStatus()) || !"BANK_TRANSFER".equals(order.getPaymentMethod())
                    || "PAID".equals(order.getPaymentStatus())) {
                em.getTransaction().rollback();
                return false;
            }
            restoreStock(em, orderId);
            order.setOrderStatus("WAITING_STOCK_CONFIRM");
            order.setConfirmedAt(null);
            order.setStaff(null);
            orderStatusHistoryService.record(em, orderId, staffId, "STAFF", "PENDING", "WAITING_STOCK_CONFIRM", reason);
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            em.close();
        }
    }

    public boolean cancelOrder(int orderId, Integer userId, Integer staffId, String failureReason, boolean pendingOnly) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) {
                em.getTransaction().rollback();
                return false;
            }
            if (userId != null && (order.getUser() == null || order.getUser().getUserId() != userId)) {
                em.getTransaction().rollback();
                return false;
            }
            String current = order.getOrderStatus();
            String orderCode = order.getOrderCode();
            Integer orderUserId = order.getUser() != null ? order.getUser().getUserId() : null;
            if ("CANCELLED".equals(current) || "DELIVERED".equals(current)) {
                em.getTransaction().rollback();
                return false;
            }
            if (pendingOnly && !"PENDING".equals(current) && !"WAITING_STOCK_CONFIRM".equals(current)) {
                em.getTransaction().rollback();
                return false;
            }
            if ("READY".equals(current) && staffId == null && userId != null) {
                em.getTransaction().rollback();
                return false;
            }

            boolean stockWasDeducted = !"WAITING_STOCK_CONFIRM".equals(current);
            if (stockWasDeducted) restoreStock(em, orderId);
            revertCoupon(em, orderId);
            order.setOrderStatus("CANCELLED");
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelledBy(staffId != null ? "STAFF" : "CUSTOMER");
            if ("PAID".equals(order.getPaymentStatus())) order.setRefundStatus("PENDING");
            if (failureReason != null && !failureReason.isBlank()) order.setFailureReason(failureReason);
            if (staffId != null) {
                User staff = em.find(User.class, staffId);
                if (staff != null) order.setStaff(staff);
            }

            String actor = staffId != null ? "STAFF" : "USER";
            orderStatusHistoryService.record(em, orderId, staffId != null ? staffId : userId, actor, current, "CANCELLED", failureReason != null ? failureReason : "Hủy đơn");
            em.getTransaction().commit();
            if (staffId != null && orderUserId != null) {
                notificationService.notifyUser(orderUserId, "Đơn hàng đã hủy", "Đơn " + orderCode + " đã bị hủy", "ORDER_CANCELLED", "/account/orders/" + orderId);
            } else {
                notificationService.notifyRole("STAFF", "Khách hủy đơn", "Đơn " + orderCode + " đã bị khách hủy", "ORDER_CANCELLED", "/staff/orders/" + orderId);
                notificationService.notifyRole("ADMIN", "Khách hủy đơn", "Đơn " + orderCode + " đã bị khách hủy", "ORDER_CANCELLED", "/admin/orders");
            }
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void clearCart(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Cart cart = findCart(em, userId);
            if (cart != null) {
                em.createQuery("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
                        .setParameter("cartId", cart.getCartId())
                        .executeUpdate();
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private Cart findCart(EntityManager em, int userId) {
        List<Cart> carts = em.createQuery("SELECT c FROM Cart c WHERE c.user.userId = :userId", Cart.class)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultList();
        return carts.isEmpty() ? null : carts.get(0);
    }

    private List<CartItem> findCartItems(EntityManager em, int cartId) {
        return em.createQuery("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = :cartId ORDER BY ci.cartItemId", CartItem.class)
                .setParameter("cartId", cartId)
                .getResultList();
    }

    private Map<Integer, CheckoutLine> validateStock(EntityManager em, Map<Integer, Integer> quantities, Map<Integer, Integer> productIds) {
        Map<Integer, CheckoutLine> lines = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            int variantId = entry.getKey();
            int qty = entry.getValue();
            ProductVariant variant = em.find(ProductVariant.class, variantId);
            if (variant == null || variant.getProduct() == null) throw new IllegalArgumentException("Sản phẩm không hợp lệ");
            Product product = variant.getProduct();
            if (productIds != null && productIds.get(variantId) != product.getProductId()) throw new IllegalArgumentException("Sản phẩm không hợp lệ");
            if (!"AVAILABLE".equals(product.getStatus()) || !"AVAILABLE".equals(variant.getStatus())) throw new IllegalArgumentException(product.getName() + " hiện không còn bán");
            Integer stock = variant.getQuantityAvailable();
            if (stock != null && stock < qty) throw new IllegalArgumentException(product.getName() + " chỉ còn " + stock + " phần");
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getBasePrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Giá sản phẩm không hợp lệ");

            CheckoutLine line = new CheckoutLine();
            line.product = product;
            line.variant = variant;
            line.quantity = qty;
            line.unitPrice = unitPrice;
            lines.put(variantId, line);
        }
        return lines;
    }

    private Map<Integer, CheckoutLine> lockAndDeduct(EntityManager em, Map<Integer, Integer> quantities, Map<Integer, Integer> productIds) {
        Map<Integer, CheckoutLine> lines = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            int variantId = entry.getKey();
            int qty = entry.getValue();
            ProductVariant variant = em.find(ProductVariant.class, variantId, LockModeType.PESSIMISTIC_WRITE);
            if (variant == null || variant.getProduct() == null) throw new IllegalArgumentException("Sản phẩm không hợp lệ");
            Product product = variant.getProduct();
            if (productIds != null && productIds.get(variantId) != product.getProductId()) throw new IllegalArgumentException("Sản phẩm không hợp lệ");
            if (!"AVAILABLE".equals(product.getStatus()) || !"AVAILABLE".equals(variant.getStatus())) throw new IllegalArgumentException(product.getName() + " hiện không còn bán");
            Integer stock = variant.getQuantityAvailable();
            if (stock != null && stock < qty) throw new IllegalArgumentException(product.getName() + " chỉ còn " + stock + " phần");
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getBasePrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Giá sản phẩm không hợp lệ");
            if (stock != null) variant.setQuantityAvailable(stock - qty);

            CheckoutLine line = new CheckoutLine();
            line.product = product;
            line.variant = variant;
            line.quantity = qty;
            line.unitPrice = unitPrice;
            lines.put(variantId, line);
        }
        return lines;
    }

    private CouponResult verifyCoupon(EntityManager em, String couponCode, BigDecimal totalAmount, BigDecimal shippingFee, Integer userId) {
        CouponResult result = new CouponResult();
        if (couponCode == null || couponCode.trim().isEmpty()) return result;

        String code = couponCode.trim().toUpperCase();
        List<Coupon> coupons = em.createQuery("SELECT c FROM Coupon c WHERE c.code = :code", Coupon.class)
                .setParameter("code", code)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (coupons.isEmpty()) throw new IllegalArgumentException("Mã giảm giá không tồn tại");

        Coupon coupon = coupons.get(0);
        if (!Boolean.TRUE.equals(coupon.getIsActive())) throw new IllegalArgumentException("Mã giảm giá đã bị vô hiệu hóa");
        if (coupon.getExpiresAt() != null && !coupon.getExpiresAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Mã giảm giá đã hết hạn");
        if (coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses()) throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng");
        BigDecimal minOrder = coupon.getMinOrder() != null ? coupon.getMinOrder() : BigDecimal.ZERO;
        if (totalAmount.compareTo(minOrder) < 0) throw new IllegalArgumentException("Đơn hàng tối thiểu " + minOrder.toPlainString() + "đ");
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Vui lòng đăng nhập và nhận mã trước khi sử dụng");
        }
        List<CouponRedemption> claims = em.createQuery(
                        "SELECT cr FROM CouponRedemption cr WHERE cr.couponId = :couponId AND cr.userId = :userId AND cr.usedAt IS NULL", CouponRedemption.class)
                .setParameter("couponId", coupon.getCouponId())
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (claims.isEmpty()) throw new IllegalArgumentException("Mã không có trong ví hoặc đã được sử dụng");
        Long used = em.createQuery("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.couponId = :couponId AND cr.userId = :userId AND cr.usedAt IS NOT NULL", Long.class)
                .setParameter("couponId", coupon.getCouponId())
                .setParameter("userId", userId)
                .getSingleResult();
        if (used > 0) throw new IllegalArgumentException("Bạn đã sử dụng mã này rồi");

        result.coupon = coupon;
        result.redemption = claims.get(0);
        result.discount = couponService.calculateDiscount(coupon, totalAmount, shippingFee);
        result.code = coupon.getCode();
        return result;
    }

    private void applyCoupon(EntityManager em, CouponResult coupon, int orderId, Integer userId) {
        if (coupon.coupon == null) return;
        coupon.redemption.setOrderId(orderId);
        coupon.redemption.setUsedAt(LocalDateTime.now());
        coupon.redemption.setDiscountAmount(coupon.discount);
        coupon.coupon.setUsedCount(coupon.coupon.getUsedCount() + 1);
    }

    private Orders buildOrder(String prefix, User user, String customerName, String phone, String address, String deliveryNote, String paymentMethod,
                               Integer ghnProvinceId, Integer ghnDistrictId, String ghnWardCode, String toProvinceName, String toDistrictName, String toWardName,
                                BigDecimal shippingFee, BigDecimal serviceFee, BigDecimal totalAmount, BigDecimal finalAmount, BigDecimal discountAmount, String couponCode) {
        Orders order = new Orders();
        order.setOrderCode(prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setCustomerName(customerName);
        order.setCustomerPhone(phone);
        order.setCustomerAddress(address);
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setServiceFee(serviceFee);
        order.setFinalAmount(finalAmount);
        order.setGhnProvinceId(ghnProvinceId);
        order.setGhnDistrictId(ghnDistrictId);
        order.setGhnWardCode(ghnWardCode);
        order.setToProvinceName(toProvinceName);
        order.setToDistrictName(toDistrictName);
        order.setToWardName(toWardName);
        order.setShippingProvider("INTERNAL");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode);
        order.setDeliveryNote(deliveryNote);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private BigDecimal calculateGhnFee(List<CheckoutLine> lines, Integer districtId, String wardCode) {
        if (districtId == null || wardCode == null || wardCode.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ địa chỉ GHN để tính phí giao hàng");
        }

        int weight = 0;
        int length = 0;
        int width = 0;
        int height = 0;
        for (CheckoutLine line : lines) {
            ProductVariant variant = line.variant;
            weight += positiveInt(variant.getWeight(), 500) * line.quantity;
            length = Math.max(length, positiveInt(variant.getLength(), 20));
            width = Math.max(width, positiveInt(variant.getWidth(), 20));
            height += positiveInt(variant.getHeight(), 10) * line.quantity;
        }

        Map<String, Object> quote = shippingService.calculateFee(districtId, wardCode, weight, length, width, height);
        Object fee = quote.get("fee");
        if (!(fee instanceof Number)) {
            throw new IllegalArgumentException("Không thể tính phí giao hàng từ GHN. Vui lòng thử lại");
        }
        return BigDecimal.valueOf(((Number) fee).doubleValue());
    }

    private int positiveInt(BigDecimal value, int fallback) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value.intValue() : fallback;
    }

    private String modifierIds(Map<String, Object> itemData) {
        Object raw = itemData.get("modifierOptionIds");
        if (!(raw instanceof List<?> ids)) return "";
        return ids.stream().map(id -> { if (!(id instanceof Number)) throw new IllegalArgumentException("Tùy chọn sản phẩm không hợp lệ"); return String.valueOf(((Number) id).intValue()); }).distinct().collect(java.util.stream.Collectors.joining(","));
    }

    private List<ProductModifierOption> modifiers(EntityManager em, String selectedIds, int productId) {
        List<ProductModifierOption> result = new ArrayList<>();
        if (selectedIds == null || selectedIds.isBlank()) return result;
        for (String id : selectedIds.split(",")) {
            ProductModifierOption option;
            try { option = em.find(ProductModifierOption.class, Integer.parseInt(id)); } catch (NumberFormatException e) { throw new IllegalArgumentException("Tùy chọn sản phẩm không hợp lệ"); }
            if (option == null || !Boolean.TRUE.equals(option.getIsActive()) || option.getGroup() == null || !Boolean.TRUE.equals(option.getGroup().getIsActive()) || option.getGroup().getProduct().getProductId() != productId) throw new IllegalArgumentException("Tùy chọn sản phẩm không hợp lệ");
            result.add(option);
        }
        for (var group : em.createQuery("SELECT g FROM ProductModifierGroup g WHERE g.product.productId = :productId AND g.isActive = true", entity.ProductModifierGroup.class).setParameter("productId", productId).getResultList()) {
            int count = (int) result.stream().filter(o -> o.getGroup().getModifierGroupId() == group.getModifierGroupId()).count();
            if (count < group.getMinSelections() || count > group.getMaxSelections()) throw new IllegalArgumentException("Số lượng tùy chọn không hợp lệ");
        }
        return result;
    }

    private void persistItems(EntityManager em, Orders order, List<CheckoutLine> lines) {
        for (CheckoutLine line : lines) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(line.product);
            item.setVariant(line.variant);
            item.setProductName(line.product.getName());
            item.setVariantName(line.variant != null ? line.variant.getVariantName() : "");
            item.setQuantity(line.quantity);
            item.setUnitPrice(line.unitPrice);
            item.setTotalPrice(line.unitPrice.multiply(BigDecimal.valueOf(line.quantity)));
            em.persist(item);
            List<OrderItem.ModifierItem> modifierItems = new ArrayList<>();
            for (ProductModifierOption option : line.modifiers) {
                modifierItems.add(new OrderItem.ModifierItem(option.getModifierOptionId(), option.getGroup().getName(), option.getName(), option.getPrice() != null ? option.getPrice() : BigDecimal.ZERO));
            }
            item.setModifiers(modifierItems);
        }
    }

    private void revertCoupon(EntityManager em, int orderId) {
        CouponRedemption cr = em.createQuery("SELECT cr FROM CouponRedemption cr WHERE cr.orderId = :orderId", CouponRedemption.class)
                .setParameter("orderId", orderId).setMaxResults(1).getResultStream().findFirst().orElse(null);
        if (cr == null) return;
        Coupon coupon = em.find(Coupon.class, cr.getCouponId());
        if (coupon != null && coupon.getUsedCount() > 0) {
            coupon.setUsedCount(coupon.getUsedCount() - 1);
        }
        cr.setUsedAt(null);
        cr.setOrderId(null);
    }

    private void restoreStock(EntityManager em, int orderId) {
        List<OrderItem> items = em.createQuery("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId", OrderItem.class)
                .setParameter("orderId", orderId)
                .getResultList();
        for (OrderItem item : items) {
            if (item.getVariant() == null) continue;
            ProductVariant variant = em.find(ProductVariant.class, item.getVariant().getVariantId(), LockModeType.PESSIMISTIC_WRITE);
            if (variant != null && variant.getQuantityAvailable() != null) {
                variant.setQuantityAvailable(variant.getQuantityAvailable() + item.getQuantity());
            }
        }
    }

    private BigDecimal validateBusinessHoursAndGetServiceFee(Map<String, String> config) {
        String openTime = config.getOrDefault(StoreConfigService.OPEN_TIME, "08:00");
        String closeTime = config.getOrDefault(StoreConfigService.CLOSE_TIME, "22:00");
        if (!StoreConfigService.isOpen(openTime, closeTime, LocalTime.now())) throw new IllegalArgumentException("Cửa hàng hiện đã đóng cửa");
        return StoreConfigService.parseFee(config.get(StoreConfigService.SERVICE_FEE));
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.trim().matches("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    }

    private boolean isValidAddress(String address) {
        String value = address == null ? "" : address.trim();
        return value.length() >= 5 && value.length() <= 500;
    }
}
