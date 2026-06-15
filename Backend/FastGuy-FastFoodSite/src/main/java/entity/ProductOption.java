package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ProductOption")
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "option_name", nullable = false, length = 255)
    private String optionName;

    @Column(name = "extra_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(name = "stock_controlled", nullable = false)
    private Boolean stockControlled = false;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    public ProductOption() {}

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
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
