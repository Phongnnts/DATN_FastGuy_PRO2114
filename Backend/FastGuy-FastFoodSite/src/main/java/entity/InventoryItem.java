package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "InventoryItem")
public class InventoryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_item_id") private int inventoryItemId;
    @Column(name = "name", nullable = false) private String name;
    @Column(name = "item_type", nullable = false) private String itemType;
    @Column(name = "base_unit", nullable = false) private String baseUnit;
    @Column(name = "on_hand_quantity", nullable = false, precision = 19, scale = 4) private BigDecimal onHandQuantity = BigDecimal.ZERO;
    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4) private BigDecimal reservedQuantity = BigDecimal.ZERO;
    @Column(name = "minimum_quantity", nullable = false, precision = 19, scale = 4) private BigDecimal minimumQuantity = BigDecimal.ZERO;
    @Column(name = "inventory_code", nullable = false) private String inventoryCode;
    @Column(name = "count_frequency", nullable = false) private String countFrequency = "WEEKLY";
    @Column(name = "average_unit_cost", nullable = false, precision = 19, scale = 4) private BigDecimal averageUnitCost = BigDecimal.ZERO;
    @Column(name = "last_counted_at", columnDefinition = "datetime2(0)") private LocalDateTime lastCountedAt;
    @Column(name = "active", nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime2(0)") private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime2(0)") private LocalDateTime updatedAt;

    @PrePersist void prePersist() { createdAt = updatedAt = LocalDateTime.now().withNano(0); }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now().withNano(0); }

    public BigDecimal availableQuantity() { return onHandQuantity.subtract(reservedQuantity).max(BigDecimal.ZERO).setScale(4); }
    public void reserve(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0 || quantity.compareTo(availableQuantity()) > 0) throw new IllegalStateException("Invalid reservation quantity");
        reservedQuantity = reservedQuantity.add(quantity);
    }
    public void release(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0 || quantity.compareTo(reservedQuantity) > 0) throw new IllegalStateException("Invalid release quantity");
        reservedQuantity = reservedQuantity.subtract(quantity);
    }
    public int getInventoryItemId() { return inventoryItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
    public BigDecimal getOnHandQuantity() { return onHandQuantity; }
    public void setOnHandQuantity(BigDecimal quantity) { if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) throw new IllegalStateException("Invalid on-hand quantity"); onHandQuantity = quantity; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(BigDecimal quantity) { if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) throw new IllegalStateException("Invalid reserved quantity"); reservedQuantity = quantity; }
    public BigDecimal getMinimumQuantity() { return minimumQuantity; }
    public void setMinimumQuantity(BigDecimal quantity) { if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) throw new IllegalStateException("Invalid minimum quantity"); minimumQuantity = quantity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getInventoryCode() { return inventoryCode; }
    public void setInventoryCode(String inventoryCode) { this.inventoryCode = inventoryCode; }
    public String getCountFrequency() { return countFrequency; }
    public void setCountFrequency(String countFrequency) { this.countFrequency = countFrequency; }
    public BigDecimal getAverageUnitCost() { return averageUnitCost; }
    public void setAverageUnitCost(BigDecimal cost) { if(cost==null||cost.signum()<0)throw new IllegalStateException("Invalid average unit cost"); averageUnitCost=cost; }
    public LocalDateTime getLastCountedAt() { return lastCountedAt; }
    public void setLastCountedAt(LocalDateTime value) { lastCountedAt=value; }
}
