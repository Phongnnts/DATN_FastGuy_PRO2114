package entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "InventoryReservationItem", uniqueConstraints = @UniqueConstraint(columnNames = {"reservation_id", "inventory_item_id"}))
public class InventoryReservationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_item_id") private int reservationItemId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reservation_id", nullable = false) private InventoryReservation reservation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "inventory_item_id", nullable = false) private InventoryItem inventoryItem;
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4) private BigDecimal quantity;

    public int getReservationItemId() { return reservationItemId; }
    public InventoryReservation getReservation() { return reservation; }
    public void setReservation(InventoryReservation reservation) { this.reservation = reservation; }
    public InventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(InventoryItem inventoryItem) { this.inventoryItem = inventoryItem; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("Invalid reservation item quantity"); this.quantity = quantity; }
}
