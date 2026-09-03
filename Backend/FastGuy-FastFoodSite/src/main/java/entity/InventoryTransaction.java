package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "InventoryTransaction")
@Check(
    constraints = "quantity <> 0 AND transaction_type IN ('RECEIPT', 'RESERVE', 'RELEASE', 'CONSUME', 'ADJUSTMENT', 'WASTE', 'RETURN')"
)
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_transaction_id")
    private int inventoryTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "note")
    private String note;

    @Column(name = "quantity_before", precision = 19, scale = 4)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", precision = 19, scale = 4)
    private BigDecimal quantityAfter;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "unit_cost_snapshot", precision = 19, scale = 4)
    private BigDecimal unitCostSnapshot;

    @Column(name = "total_cost", precision = 19, scale = 4)
    private BigDecimal totalCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id")
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id")
    private StockCount stockCount;

    @Column(
        name = "created_at",
        nullable = false,
        columnDefinition = "datetime2(0)"
    )
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now().withNano(0);
    }

    public int getInventoryTransactionId() {
        return inventoryTransactionId;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public Orders getOrder() {
        return order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        if (
            quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0
        ) throw new IllegalStateException("Invalid transaction quantity");
        this.quantity = quantity;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(BigDecimal quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(BigDecimal quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal value) {
        unitCostSnapshot = value;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal value) {
        totalCost = value;
    }

    public GoodsReceipt getGoodsReceipt() {
        return goodsReceipt;
    }

    public void setGoodsReceipt(GoodsReceipt value) {
        goodsReceipt = value;
    }

    public StockCount getStockCount() {
        return stockCount;
    }

    public void setStockCount(StockCount value) {
        stockCount = value;
    }
}
