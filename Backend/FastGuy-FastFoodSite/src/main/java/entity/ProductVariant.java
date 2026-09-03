package entity;

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
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ProductVariant")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private int variantId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    @Column(name = "inventory_mode", nullable = false)
    private String inventoryMode = "UNTRACKED";

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "length")
    private BigDecimal length;

    @Column(name = "width")
    private BigDecimal width;

    @Column(name = "height")
    private BigDecimal height;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean updatedAtAdvanced;

    public ProductVariant() {}

    public int getVariantId() {
        return variantId;
    }

    public void setVariantId(int variantId) {
        this.variantId = variantId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public String getInventoryMode() {
        return inventoryMode;
    }

    public void setInventoryMode(String inventoryMode) {
        this.inventoryMode = inventoryMode;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getLength() {
        return length;
    }

    public void setLength(BigDecimal length) {
        this.length = length;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void advanceUpdatedAt() {
        updatedAt = nextUpdatedAt();
        updatedAtAdvanced = true;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        updatedAtAdvanced = false;
    }

    @PreUpdate
    void preUpdate() {
        if (updatedAtAdvanced) updatedAtAdvanced = false;
        else updatedAt = nextUpdatedAt();
    }

    private LocalDateTime nextUpdatedAt() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (updatedAt == null) return now;
        LocalDateTime next = updatedAt.plusSeconds(1);
        return now.isAfter(next) ? now : next;
    }
}
