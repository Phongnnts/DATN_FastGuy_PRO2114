package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "Recipe", uniqueConstraints = @UniqueConstraint(columnNames = "variant_id"))
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id") private int recipeId;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id", nullable = false) private ProductVariant variant;
    @Column(name = "yield_quantity", nullable = false, precision = 19, scale = 4) private BigDecimal yieldQuantity = BigDecimal.ONE;
    @Column(name = "active", nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime2(0)") private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime2(0)") private LocalDateTime updatedAt;
    @Transient private boolean updatedAtAdvanced;
    @OneToMany(mappedBy = "recipe") private List<RecipeItem> items = new ArrayList<>();

    @PrePersist void prePersist() { createdAt = updatedAt = LocalDateTime.now().withNano(0); updatedAtAdvanced=false; }
    @PreUpdate void preUpdate() { if(updatedAtAdvanced)updatedAtAdvanced=false;else updatedAt=nextUpdatedAt(); }

    public int getRecipeId() { return recipeId; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public BigDecimal getYieldQuantity() { return yieldQuantity; }
    public void setYieldQuantity(BigDecimal quantity) { if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("Invalid yield quantity"); yieldQuantity = quantity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<RecipeItem> getItems() { return items; }
    public void setItems(List<RecipeItem> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void advanceUpdatedAt() { updatedAt=nextUpdatedAt();updatedAtAdvanced=true; }
    private LocalDateTime nextUpdatedAt() { LocalDateTime now=LocalDateTime.now().withNano(0);if(updatedAt==null)return now;LocalDateTime next=updatedAt.plusSeconds(1);return now.isAfter(next)?now:next; }
}
