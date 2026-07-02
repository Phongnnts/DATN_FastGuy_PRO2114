package entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ClaimedCoupon")
public class ClaimedCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claimed_id")
    private int claimedId;

    @Column(name = "coupon_id")
    private int couponId;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public ClaimedCoupon() {}

    public int getClaimedId() { return claimedId; }
    public void setClaimedId(int claimedId) { this.claimedId = claimedId; }
    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
