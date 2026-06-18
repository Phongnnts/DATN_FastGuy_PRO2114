package entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ProductOption")
public class ProductOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private int optionId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "option_name")
    private String optionName;

    @Column(name = "extra_price")
    private BigDecimal extraPrice;

    @Column(name = "stock_controlled")
    private Boolean stockControlled;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    public ProductOption() {}

    public int getOptionId() { return optionId; }
    public void setOptionId(int optionId) { this.optionId = optionId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public Boolean getStockControlled() { return stockControlled; }
    public void setStockControlled(Boolean stockControlled) { this.stockControlled = stockControlled; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
}
