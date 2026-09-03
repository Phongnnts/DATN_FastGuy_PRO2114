package entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name="StockCountItem",uniqueConstraints=@UniqueConstraint(columnNames={"stock_count_id","inventory_item_id"}))
public class StockCountItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="stock_count_item_id") private int stockCountItemId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="stock_count_id",nullable=false) private StockCount stockCount;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="inventory_item_id",nullable=false) private InventoryItem inventoryItem;
    @Column(name="theoretical_quantity",nullable=false,precision=19,scale=4) private BigDecimal theoreticalQuantity;
    @Column(name="actual_quantity",precision=19,scale=4) private BigDecimal actualQuantity;
    @Column(name="variance_quantity",precision=19,scale=4) private BigDecimal varianceQuantity;
    @Column(name="unit_cost_snapshot",precision=19,scale=4) private BigDecimal unitCostSnapshot;
    @Column(name="reserved_quantity_snapshot",nullable=false,precision=19,scale=4) private BigDecimal reservedQuantitySnapshot = BigDecimal.ZERO;
    @Column(name="variance_cost",precision=19,scale=4) private BigDecimal varianceCost;
    @Column(name="reason_code") private String reasonCode;
    @Column(name="note") private String note;
    public int getStockCountItemId(){return stockCountItemId;} public StockCount getStockCount(){return stockCount;} public void setStockCount(StockCount v){stockCount=v;} public InventoryItem getInventoryItem(){return inventoryItem;} public void setInventoryItem(InventoryItem v){inventoryItem=v;}
    public BigDecimal getTheoreticalQuantity(){return theoreticalQuantity;} public void setTheoreticalQuantity(BigDecimal v){theoreticalQuantity=v;} public BigDecimal getActualQuantity(){return actualQuantity;} public void setActualQuantity(BigDecimal v){actualQuantity=v;}
    public BigDecimal getVarianceQuantity(){return varianceQuantity;} public void setVarianceQuantity(BigDecimal v){varianceQuantity=v;} public BigDecimal getUnitCostSnapshot(){return unitCostSnapshot;} public void setUnitCostSnapshot(BigDecimal v){unitCostSnapshot=v;}
    public BigDecimal getReservedQuantitySnapshot(){return reservedQuantitySnapshot;} public void setReservedQuantitySnapshot(BigDecimal v){reservedQuantitySnapshot=v;}
    public BigDecimal getVarianceCost(){return varianceCost;} public void setVarianceCost(BigDecimal v){varianceCost=v;} public String getReasonCode(){return reasonCode;} public void setReasonCode(String v){reasonCode=v;} public String getNote(){return note;} public void setNote(String v){note=v;}
}
