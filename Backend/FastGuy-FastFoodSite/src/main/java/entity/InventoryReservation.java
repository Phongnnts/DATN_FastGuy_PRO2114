package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "InventoryReservation", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
@Check(constraints = "status IN ('RESERVED', 'CONSUMED', 'RELEASED', 'WASTED')")
public class InventoryReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id") private int reservationId;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private Orders order;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime2(0)") private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime2(0)") private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "reservation") private List<InventoryReservationItem> items = new ArrayList<>();

    @PrePersist void prePersist() { createdAt = updatedAt = LocalDateTime.now().withNano(0); }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now().withNano(0); }
    public int getReservationId() { return reservationId; }
    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<InventoryReservationItem> getItems() { return items; }
    public void setItems(List<InventoryReservationItem> items) { this.items = items; }
}
