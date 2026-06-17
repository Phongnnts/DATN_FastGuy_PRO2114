package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "DeliveryZone")
public class DeliveryZone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private int zoneId;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @Column(name = "is_active")
    private Boolean isActive;

    public DeliveryZone() {}

    public int getZoneId() { return zoneId; }
    public void setZoneId(int zoneId) { this.zoneId = zoneId; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
