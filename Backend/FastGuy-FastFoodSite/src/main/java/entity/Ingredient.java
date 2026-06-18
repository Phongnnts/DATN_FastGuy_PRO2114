package entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Ingredient")
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private int ingredientId;

    @Column(name = "name")
    private String name;

    @Column(name = "unit")
    private String unit;

    @Column(name = "stock_quantity")
    private BigDecimal stockQuantity;

    @Column(name = "min_stock_threshold")
    private BigDecimal minStockThreshold;

    @Column(name = "status")
    private String status;

    public Ingredient() {}

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
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
