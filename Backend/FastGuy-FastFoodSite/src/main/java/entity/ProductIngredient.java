package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ProductIngredient")
public class ProductIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_ingredient_id")
    private int productIngredientId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(name = "quantity_required")
    private BigDecimal quantityRequired;

    public ProductIngredient() {}

    public int getProductIngredientId() { return productIngredientId; }
    public void setProductIngredientId(int productIngredientId) { this.productIngredientId = productIngredientId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public BigDecimal getQuantityRequired() { return quantityRequired; }
    public void setQuantityRequired(BigDecimal quantityRequired) { this.quantityRequired = quantityRequired; }
}
