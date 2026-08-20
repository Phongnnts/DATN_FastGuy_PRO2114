package entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ProductCombo")
public class ProductCombo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "combo_id")
    private int comboId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "homepage_occasion")
    private String homepageOccasion;

    @Column(name = "homepage_sort_order")
    private int homepageSortOrder;

    public int getComboId() { return comboId; }
    public void setComboId(int comboId) { this.comboId = comboId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getHomepageOccasion() { return homepageOccasion; }
    public void setHomepageOccasion(String homepageOccasion) { this.homepageOccasion = homepageOccasion; }
    public int getHomepageSortOrder() { return homepageSortOrder; }
    public void setHomepageSortOrder(int homepageSortOrder) { this.homepageSortOrder = homepageSortOrder; }
}
