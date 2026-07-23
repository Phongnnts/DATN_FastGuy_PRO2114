package entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "InventoryTransaction")
public class InventoryTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_transaction_id")
    private int inventoryTransactionId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false)
    private Orders order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;
    @Column(name = "transaction_type") private String transactionType;
    @Column(name = "quantity") private int quantity;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
    public void setOrder(Orders order) { this.order = order; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
