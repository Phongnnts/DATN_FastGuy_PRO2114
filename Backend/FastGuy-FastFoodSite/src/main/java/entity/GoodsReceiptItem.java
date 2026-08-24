package entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name="GoodsReceiptItem",uniqueConstraints=@UniqueConstraint(columnNames={"goods_receipt_id","inventory_item_id"}))
public class GoodsReceiptItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="goods_receipt_item_id") private int goodsReceiptItemId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="goods_receipt_id",nullable=false) private GoodsReceipt receipt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="inventory_item_id",nullable=false) private InventoryItem inventoryItem;
    @Column(name="purchase_quantity",nullable=false,precision=19,scale=4) private BigDecimal purchaseQuantity;
    @Column(name="purchase_unit",nullable=false) private String purchaseUnit;
    @Column(name="conversion_factor",nullable=false,precision=19,scale=4) private BigDecimal conversionFactor;
    @Column(name="base_quantity",nullable=false,precision=19,scale=4) private BigDecimal baseQuantity;
    @Column(name="purchase_unit_price",nullable=false,precision=19,scale=4) private BigDecimal purchaseUnitPrice;
    @Column(name="line_total",nullable=false,precision=19,scale=4) private BigDecimal lineTotal;
    @Column(name="average_cost_before",precision=19,scale=4) private BigDecimal averageCostBefore;
    @Column(name="average_cost_after",precision=19,scale=4) private BigDecimal averageCostAfter;
    public int getGoodsReceiptItemId(){return goodsReceiptItemId;} public GoodsReceipt getReceipt(){return receipt;} public void setReceipt(GoodsReceipt v){receipt=v;} public InventoryItem getInventoryItem(){return inventoryItem;} public void setInventoryItem(InventoryItem v){inventoryItem=v;}
    public BigDecimal getPurchaseQuantity(){return purchaseQuantity;} public void setPurchaseQuantity(BigDecimal v){purchaseQuantity=v;} public String getPurchaseUnit(){return purchaseUnit;} public void setPurchaseUnit(String v){purchaseUnit=v;}
    public BigDecimal getConversionFactor(){return conversionFactor;} public void setConversionFactor(BigDecimal v){conversionFactor=v;} public BigDecimal getBaseQuantity(){return baseQuantity;} public void setBaseQuantity(BigDecimal v){baseQuantity=v;}
    public BigDecimal getPurchaseUnitPrice(){return purchaseUnitPrice;} public void setPurchaseUnitPrice(BigDecimal v){purchaseUnitPrice=v;} public BigDecimal getLineTotal(){return lineTotal;} public void setLineTotal(BigDecimal v){lineTotal=v;}
    public BigDecimal getAverageCostBefore(){return averageCostBefore;} public void setAverageCostBefore(BigDecimal v){averageCostBefore=v;} public BigDecimal getAverageCostAfter(){return averageCostAfter;} public void setAverageCostAfter(BigDecimal v){averageCostAfter=v;}
}
