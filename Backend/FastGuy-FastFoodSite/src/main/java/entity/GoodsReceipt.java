package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name="GoodsReceipt")
public class GoodsReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="goods_receipt_id") private int goodsReceiptId;
    @Column(name="supplier_name",nullable=false) private String supplierName;
    @Column(name="invoice_number") private String invoiceNumber;
    @Column(name="received_at",nullable=false,columnDefinition="datetime2(0)") private LocalDateTime receivedAt;
    @Column(name="status",nullable=false) private String status="DRAFT";
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="approved_by") private User approvedBy;
    @Column(name="created_at",nullable=false,columnDefinition="datetime2(0)") private LocalDateTime createdAt;
    @Column(name="approved_at",columnDefinition="datetime2(0)") private LocalDateTime approvedAt;
    @OneToMany(mappedBy="receipt",cascade=CascadeType.ALL,orphanRemoval=true) private List<GoodsReceiptItem> items=new ArrayList<>();
    @PrePersist void prePersist(){if(createdAt==null)createdAt=LocalDateTime.now().withNano(0);}
    public int getGoodsReceiptId(){return goodsReceiptId;} public String getSupplierName(){return supplierName;} public void setSupplierName(String v){supplierName=v;}
    public String getInvoiceNumber(){return invoiceNumber;} public void setInvoiceNumber(String v){invoiceNumber=v;} public LocalDateTime getReceivedAt(){return receivedAt;} public void setReceivedAt(LocalDateTime v){receivedAt=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public User getCreatedBy(){return createdBy;} public void setCreatedBy(User v){createdBy=v;}
    public User getApprovedBy(){return approvedBy;} public void setApprovedBy(User v){approvedBy=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(LocalDateTime v){approvedAt=v;}
    public List<GoodsReceiptItem> getItems(){return items;} public void setItems(List<GoodsReceiptItem> v){items=v;}
}
