package service;

import dao.ClaimedCouponDAO;
import dao.CouponDAO;
import dao.CouponUsageDAO;
import entity.ClaimedCoupon;
import entity.Coupon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CouponService {
    private CouponDAO couponDAO = new CouponDAO();
    private CouponUsageDAO couponUsageDAO = new CouponUsageDAO();
    private ClaimedCouponDAO claimedCouponDAO = new ClaimedCouponDAO();

    public Map<String, Object> verify(String code, BigDecimal totalAmount, BigDecimal shippingFee, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        if (code == null || code.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "Vui lòng nhập mã giảm giá");
            return result;
        }
        code = code.trim().toUpperCase();

        Coupon coupon = couponDAO.findByCode(code);
        if (coupon == null) {
            result.put("valid", false);
            result.put("message", "Mã giảm giá không tồn tại");
            return result;
        }

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            result.put("valid", false);
            result.put("message", "Mã giảm giá đã bị vô hiệu hóa");
            return result;
        }

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            result.put("valid", false);
            result.put("message", "Mã giảm giá đã hết hạn");
            return result;
        }

        if (coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses()) {
            result.put("valid", false);
            result.put("message", "Mã giảm giá đã hết lượt sử dụng");
            return result;
        }

        BigDecimal minOrder = coupon.getMinOrder() != null ? coupon.getMinOrder() : BigDecimal.ZERO;
        if (totalAmount.compareTo(minOrder) < 0) {
            result.put("valid", false);
            result.put("message", "Đơn hàng tối thiểu " + minOrder.toPlainString() + "đ");
            return result;
        }

        if (userId == null || userId <= 0) {
            result.put("valid", false);
            result.put("message", "Vui lòng đăng nhập và nhận mã trước khi sử dụng");
            return result;
        }
        if (!claimedCouponDAO.hasUnused(coupon.getCouponId(), userId)) {
            result.put("valid", false);
            result.put("message", "Mã không có trong ví hoặc đã được sử dụng");
            return result;
        }
        if (couponUsageDAO.hasUserUsed(coupon.getCouponId(), userId)) {
            result.put("valid", false);
            result.put("message", "Bạn đã sử dụng mã này rồi");
            return result;
        }

        BigDecimal discount = calculateDiscount(coupon, totalAmount, shippingFee);
        result.put("valid", true);
        result.put("couponId", coupon.getCouponId());
        result.put("code", coupon.getCode());
        result.put("type", coupon.getType());
        result.put("discount", discount);
        result.put("description", getDescription(coupon));
        return result;
    }


    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal totalAmount, BigDecimal shippingFee) {
        switch (coupon.getType()) {
            case "PERCENT": {
                BigDecimal pct = coupon.getValue().divide(BigDecimal.valueOf(100));
                BigDecimal discount = totalAmount.multiply(pct);
                if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                    discount = coupon.getMaxDiscount();
                }
                return discount;
            }
            case "FIXED": {
                return coupon.getValue().min(totalAmount);
            }
            case "FREE_SHIPPING": {
                return shippingFee;
            }
            default:
                return BigDecimal.ZERO;
        }
    }

    public String claim(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Coupon coupon = em.find(Coupon.class, couponId, LockModeType.PESSIMISTIC_WRITE);
            if (coupon == null) { em.getTransaction().rollback(); return "Mã giảm giá không tồn tại"; }
            if (!Boolean.TRUE.equals(coupon.getIsActive())) { em.getTransaction().rollback(); return "Mã giảm giá không khả dụng"; }
            if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) { em.getTransaction().rollback(); return "Mã đã hết hạn"; }
            if (coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses()) { em.getTransaction().rollback(); return "Mã đã hết lượt"; }

            boolean exists = em.createQuery("SELECT 1 FROM ClaimedCoupon c WHERE c.couponId = :cid AND c.userId = :uid", ClaimedCoupon.class)
                    .setParameter("cid", couponId).setParameter("uid", userId).setMaxResults(1).getResultList().isEmpty() == false;
            if (exists) { em.getTransaction().rollback(); return "Bạn đã nhận mã này rồi"; }

            ClaimedCoupon cc = new ClaimedCoupon();
            cc.setCouponId(couponId);
            cc.setUserId(userId);
            cc.setClaimedAt(LocalDateTime.now());
            em.persist(cc);
            em.getTransaction().commit();
            return null;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> getPublicCoupons(Integer userId) {
        List<Coupon> list = couponDAO.findPublicActive();
        Set<Integer> claimedIds = null;
        if (userId != null) {
            claimedIds = claimedCouponDAO.findByUserId(userId).stream()
                    .map(ClaimedCoupon::getCouponId).collect(Collectors.toSet());
        }
        Set<Integer> finalClaimed = claimedIds;
        return list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("couponId", c.getCouponId());
            m.put("code", c.getCode());
            m.put("type", c.getType());
            m.put("value", c.getValue());
            m.put("minOrder", c.getMinOrder());
            m.put("maxDiscount", c.getMaxDiscount());
            m.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().toString() : null);
            m.put("description", getDescription(c));
            m.put("isClaimed", finalClaimed != null && finalClaimed.contains(c.getCouponId()));
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getClaimedCoupons(int userId) {
        return claimedCouponDAO.findUnusedByUserId(userId).stream().map(cc -> {
            Coupon c = couponDAO.findById(cc.getCouponId());
            Map<String, Object> m = new HashMap<>();
            m.put("claimedId", cc.getClaimedId());
            m.put("couponId", cc.getCouponId());
            m.put("code", c != null ? c.getCode() : "");
            m.put("type", c != null ? c.getType() : "");
            m.put("value", c != null ? c.getValue() : BigDecimal.ZERO);
            m.put("minOrder", c != null ? c.getMinOrder() : BigDecimal.ZERO);
            m.put("maxDiscount", c != null ? c.getMaxDiscount() : BigDecimal.ZERO);
            m.put("description", c != null ? getDescription(c) : "");
            m.put("claimedAt", cc.getClaimedAt() != null ? cc.getClaimedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    private String getDescription(Coupon c) {
        switch (c.getType()) {
            case "PERCENT": return "Giảm " + c.getValue().intValue() + "%";
            case "FIXED": return "Giảm " + c.getValue().intValue() + "đ";
            case "FREE_SHIPPING": return "Miễn phí vận chuyển";
            default: return "";
        }
    }
}
