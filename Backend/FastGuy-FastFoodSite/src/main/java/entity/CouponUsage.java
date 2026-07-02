package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CouponUsage")
public class CouponUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_usage_id")
    private int couponUsageId;

    @Column(name = "coupon_id")
    private int couponId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "order_id")
    private int orderId;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public CouponUsage() {}

    public int getCouponUsageId() { return couponUsageId; }
    public void setCouponUsageId(int couponUsageId) { this.couponUsageId = couponUsageId; }
    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
