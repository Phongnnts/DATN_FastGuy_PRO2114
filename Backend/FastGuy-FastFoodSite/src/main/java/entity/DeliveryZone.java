package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "DeliveryZone")
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "district_name", nullable = false, unique = true, length = 100)
    private String districtName;

    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public DeliveryZone() {}

    public DeliveryZone(String districtName, BigDecimal shippingFee) {
        this.districtName = districtName;
        this.shippingFee = shippingFee;
    }

    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
