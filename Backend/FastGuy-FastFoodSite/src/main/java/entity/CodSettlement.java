package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "CodSettlement", uniqueConstraints = @UniqueConstraint(name = "UQ_CodSettlement_ShipperShift", columnNames = {"shipper_id", "shift_id"}))
public class CodSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id", nullable = false)
    private int settlementId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @ManyToOne
    @JoinColumn(name = "received_by", nullable = true)
    private User receivedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expected_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "submitted_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal submittedAmount;

    @Column(name = "verified_amount", nullable = true, precision = 18, scale = 2)
    private BigDecimal verifiedAmount;

    @Column(name = "reason", nullable = true, length = 500)
    private String reason;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "verified_at", nullable = true)
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (submittedAt == null) submittedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public int getSettlementId() { return settlementId; }
    public void setSettlementId(int settlementId) { this.settlementId = settlementId; }
    public User getShipper() { return shipper; }
    public void setShipper(User shipper) { this.shipper = shipper; }
    public WorkShift getShift() { return shift; }
    public void setShift(WorkShift shift) { this.shift = shift; }
    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public BigDecimal getSubmittedAmount() { return submittedAmount; }
    public void setSubmittedAmount(BigDecimal submittedAmount) { this.submittedAmount = submittedAmount; }
    public BigDecimal getVerifiedAmount() { return verifiedAmount; }
    public void setVerifiedAmount(BigDecimal verifiedAmount) { this.verifiedAmount = verifiedAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
