package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "stock_quantity", nullable = false, precision = 18, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(name = "min_stock_threshold", nullable = false, precision = 18, scale = 2)
    private BigDecimal minStockThreshold = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public Ingredient() {}

    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }
    public BigDecimal getMinStockThreshold() { return minStockThreshold; }
    public void setMinStockThreshold(BigDecimal minStockThreshold) { this.minStockThreshold = minStockThreshold; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
