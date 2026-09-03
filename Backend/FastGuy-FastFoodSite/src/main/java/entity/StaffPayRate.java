package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(
    name = "StaffPayRate",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_StaffPayRate_User_EffectiveFrom",
        columnNames = { "user_id", "effective_from" }
    )
)
public class StaffPayRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_rate_id")
    private int payRateId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(
        name = "regular_hourly_rate",
        nullable = false,
        precision = 18,
        scale = 2
    )
    private BigDecimal regularHourlyRate;

    @Column(
        name = "overtime_hourly_rate",
        nullable = false,
        precision = 18,
        scale = 2
    )
    private BigDecimal overtimeHourlyRate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public int getPayRateId() {
        return payRateId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User v) {
        user = v;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate v) {
        effectiveFrom = v;
    }

    public BigDecimal getRegularHourlyRate() {
        return regularHourlyRate;
    }

    public void setRegularHourlyRate(BigDecimal v) {
        regularHourlyRate = v;
    }

    public BigDecimal getOvertimeHourlyRate() {
        return overtimeHourlyRate;
    }

    public void setOvertimeHourlyRate(BigDecimal v) {
        overtimeHourlyRate = v;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User v) {
        createdBy = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
