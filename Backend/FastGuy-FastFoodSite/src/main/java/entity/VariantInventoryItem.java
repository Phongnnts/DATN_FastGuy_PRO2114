package entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "VariantInventoryItem",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "variant_id"),
        @UniqueConstraint(columnNames = "inventory_item_id"),
    }
)
public class VariantInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_inventory_item_id")
    private int variantInventoryItemId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    public int getVariantInventoryItemId() {
        return variantInventoryItemId;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
}
