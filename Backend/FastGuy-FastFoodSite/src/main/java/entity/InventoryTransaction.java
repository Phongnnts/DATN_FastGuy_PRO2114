package entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "InventoryTransaction")
public class InventoryTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_transaction_id")
    private int inventoryTransactionId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = true)
    private Orders order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = true)
    private User createdBy;
    @Column(name = "transaction_type") private String transactionType;
    @Column(name = "quantity") private int quantity;
    @Column(name = "reason_code") private String reasonCode;
    @Column(name = "note") private String note;
    @Column(name = "quantity_before") private Integer quantityBefore;
    @Column(name = "quantity_after") private Integer quantityAfter;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }

    public int getInventoryTransactionId() { return inventoryTransactionId; }
    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Integer getQuantityBefore() { return quantityBefore; }
    public void setQuantityBefore(Integer quantityBefore) { this.quantityBefore = quantityBefore; }
    public Integer getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(Integer quantityAfter) { this.quantityAfter = quantityAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
