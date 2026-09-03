package entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "StockCount")
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_count_id")
    private int stockCountId;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(name = "frequency", nullable = false)
    private String frequency;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(
        name = "created_at",
        nullable = false,
        columnDefinition = "datetime2(0)"
    )
    private LocalDateTime createdAt;

    @Column(name = "approved_at", columnDefinition = "datetime2(0)")
    private LocalDateTime approvedAt;

    @OneToMany(
        mappedBy = "stockCount",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<StockCountItem> items = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now().withNano(0);
    }

    public int getStockCountId() {
        return stockCountId;
    }

    public LocalDate getCountDate() {
        return countDate;
    }

    public void setCountDate(LocalDate v) {
        countDate = v;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String v) {
        frequency = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User v) {
        createdBy = v;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User v) {
        approvedBy = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime v) {
        approvedAt = v;
    }

    public List<StockCountItem> getItems() {
        return items;
    }

    public void setItems(List<StockCountItem> v) {
        items = v;
    }
}
