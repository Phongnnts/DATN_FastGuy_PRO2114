package entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "InventoryReservation", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "variant_id"}))
public class InventoryReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private int reservationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false)
    private Orders order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;
    @Column(name = "quantity") private int quantity;
    @Column(name = "status") private String status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
    public int getReservationId() { return reservationId; }
    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
