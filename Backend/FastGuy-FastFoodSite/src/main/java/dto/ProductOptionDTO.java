package dto;

import java.math.BigDecimal;

public class ProductOptionDTO {
    private Long optionId;
    private String optionName;
    private BigDecimal extraPrice;
    private Boolean stockControlled;
    private Integer quantityAvailable;

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public Boolean getStockControlled() { return stockControlled; }
    public void setStockControlled(Boolean stockControlled) { this.stockControlled = stockControlled; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
}
