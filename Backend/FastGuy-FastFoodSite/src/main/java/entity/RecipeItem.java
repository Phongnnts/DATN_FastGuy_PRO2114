package entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "RecipeItem", uniqueConstraints = @UniqueConstraint(columnNames = {"recipe_id", "inventory_item_id"}))
public class RecipeItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_item_id") private int recipeItemId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipe_id", nullable = false) private Recipe recipe;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "inventory_item_id", nullable = false) private InventoryItem inventoryItem;
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4) private BigDecimal quantity;

    public int getRecipeItemId() { return recipeItemId; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public InventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(InventoryItem inventoryItem) { this.inventoryItem = inventoryItem; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("Invalid recipe item quantity"); this.quantity = quantity; }
}
