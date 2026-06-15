package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ProductIngredient")
public class ProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_ingredient_id")
    private Long productIngredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "quantity_required", nullable = false, precision = 18, scale = 2)
    private BigDecimal quantityRequired;

    public ProductIngredient() {}

    public Long getProductIngredientId() { return productIngredientId; }
    public void setProductIngredientId(Long productIngredientId) { this.productIngredientId = productIngredientId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public BigDecimal getQuantityRequired() { return quantityRequired; }
    public void setQuantityRequired(BigDecimal quantityRequired) { this.quantityRequired = quantityRequired; }
}
