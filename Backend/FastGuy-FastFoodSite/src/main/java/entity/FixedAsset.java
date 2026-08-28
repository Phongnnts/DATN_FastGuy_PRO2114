package entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "FixedAsset")
public class FixedAsset {
    public enum Status { ACTIVE, RETIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "asset_id", nullable = false)
    private int assetId;
    @Column(name = "asset_name", nullable = false, length = 255) private String assetName;
    @Column(name = "acquisition_cost", nullable = false, precision = 18, scale = 2) private BigDecimal acquisitionCost;
    @Column(name = "salvage_value", nullable = false, precision = 18, scale = 2) private BigDecimal salvageValue;
    @Column(name = "depreciation_start_date", nullable = false) private LocalDate depreciationStartDate;
    @Column(name = "useful_life_months", nullable = false) private int usefulLifeMonths;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private Status status;
    @Column(name = "retired_at", nullable = true) private LocalDateTime retiredAt;
    @ManyToOne(optional = false) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void prePersist(){LocalDateTime now=LocalDateTime.now();if(createdAt==null)createdAt=now;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public int getAssetId(){return assetId;} public void setAssetId(int v){assetId=v;}
    public String getAssetName(){return assetName;} public void setAssetName(String v){assetName=v;}
    public BigDecimal getAcquisitionCost(){return acquisitionCost;} public void setAcquisitionCost(BigDecimal v){acquisitionCost=v;}
    public BigDecimal getSalvageValue(){return salvageValue;} public void setSalvageValue(BigDecimal v){salvageValue=v;}
    public LocalDate getDepreciationStartDate(){return depreciationStartDate;} public void setDepreciationStartDate(LocalDate v){depreciationStartDate=v;}
    public int getUsefulLifeMonths(){return usefulLifeMonths;} public void setUsefulLifeMonths(int v){usefulLifeMonths=v;}
    public Status getStatus(){return status;} public void setStatus(Status v){status=v;}
    public LocalDateTime getRetiredAt(){return retiredAt;} public void setRetiredAt(LocalDateTime v){retiredAt=v;}
    public User getCreatedBy(){return createdBy;} public void setCreatedBy(User v){createdBy=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
