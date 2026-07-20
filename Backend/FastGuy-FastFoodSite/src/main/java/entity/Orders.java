package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "Orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    @Column(name = "order_code")
    private String orderCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_address")
    private String customerAddress;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private DeliveryZone zone;

    @Column(name = "to_province_name")
    private String toProvinceName;

    @Column(name = "to_district_name")
    private String toDistrictName;

    @Column(name = "to_ward_name")
    private String toWardName;

    @Column(name = "ghn_province_id")
    private Integer ghnProvinceId;

    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Column(name = "ghn_ward_code")
    private String ghnWardCode;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @Column(name = "service_fee")
    private BigDecimal serviceFee;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    @Column(name = "cod_collected_amount")
    private BigDecimal codCollectedAmount;

    @Column(name = "cod_collected_at")
    private LocalDateTime codCollectedAt;

    @Column(name = "shipping_provider")
    private String shippingProvider;

    @Transient
    private Integer shippingServiceId;

    @Transient
    private Integer shippingServiceTypeId;

    @Transient
    private LocalDateTime expectedDeliveryTime;

    @Transient
    private String ghnOrderCode;

    @Transient
    private String ghnTrackingUrl;

    @Transient
    private String ghnStatus;

    @Transient
    private Integer fromDistrictId;

    @Transient
    private String fromWardCode;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "payos_payment_link_id")
    private String payosPaymentLinkId;

    @Column(name = "payos_checkout_url")
    private String payosCheckoutUrl;

    @Column(name = "order_status")
    private String orderStatus;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User staff;

    @ManyToOne
    @JoinColumn(name = "shipper_id")
    private User shipper;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "refund_status")
    private String refundStatus;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_note")
    private String refundNote;

    @Column(name = "internal_note")
    private String internalNote;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "delivery_note")
    private String deliveryNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Orders() {}

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public DeliveryZone getZone() { return zone; }
    public void setZone(DeliveryZone zone) { this.zone = zone; }
    public String getToProvinceName() { return toProvinceName; }
    public void setToProvinceName(String toProvinceName) { this.toProvinceName = toProvinceName; }
    public String getToDistrictName() { return toDistrictName; }
    public void setToDistrictName(String toDistrictName) { this.toDistrictName = toDistrictName; }
    public String getToWardName() { return toWardName; }
    public void setToWardName(String toWardName) { this.toWardName = toWardName; }
    public Integer getGhnProvinceId() { return ghnProvinceId; }
    public void setGhnProvinceId(Integer ghnProvinceId) { this.ghnProvinceId = ghnProvinceId; }
    public Integer getGhnDistrictId() { return ghnDistrictId; }
    public void setGhnDistrictId(Integer ghnDistrictId) { this.ghnDistrictId = ghnDistrictId; }
    public String getGhnWardCode() { return ghnWardCode; }
    public void setGhnWardCode(String ghnWardCode) { this.ghnWardCode = ghnWardCode; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public BigDecimal getCodCollectedAmount() { return codCollectedAmount; }
    public void setCodCollectedAmount(BigDecimal codCollectedAmount) { this.codCollectedAmount = codCollectedAmount; }
    public LocalDateTime getCodCollectedAt() { return codCollectedAt; }
    public void setCodCollectedAt(LocalDateTime codCollectedAt) { this.codCollectedAt = codCollectedAt; }
    public String getShippingProvider() { return shippingProvider; }
    public void setShippingProvider(String shippingProvider) { this.shippingProvider = shippingProvider; }
    public Integer getShippingServiceId() { return shippingServiceId; }
    public void setShippingServiceId(Integer shippingServiceId) { this.shippingServiceId = shippingServiceId; }
    public Integer getShippingServiceTypeId() { return shippingServiceTypeId; }
    public void setShippingServiceTypeId(Integer shippingServiceTypeId) { this.shippingServiceTypeId = shippingServiceTypeId; }
    public LocalDateTime getExpectedDeliveryTime() { return expectedDeliveryTime; }
    public void setExpectedDeliveryTime(LocalDateTime expectedDeliveryTime) { this.expectedDeliveryTime = expectedDeliveryTime; }
    public String getGhnOrderCode() { return ghnOrderCode; }
    public void setGhnOrderCode(String ghnOrderCode) { this.ghnOrderCode = ghnOrderCode; }
    public String getGhnTrackingUrl() { return ghnTrackingUrl; }
    public void setGhnTrackingUrl(String ghnTrackingUrl) { this.ghnTrackingUrl = ghnTrackingUrl; }
    public String getGhnStatus() { return ghnStatus; }
    public void setGhnStatus(String ghnStatus) { this.ghnStatus = ghnStatus; }
    public Integer getFromDistrictId() { return fromDistrictId; }
    public void setFromDistrictId(Integer fromDistrictId) { this.fromDistrictId = fromDistrictId; }
    public String getFromWardCode() { return fromWardCode; }
    public void setFromWardCode(String fromWardCode) { this.fromWardCode = fromWardCode; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPayosPaymentLinkId() { return payosPaymentLinkId; }
    public void setPayosPaymentLinkId(String payosPaymentLinkId) { this.payosPaymentLinkId = payosPaymentLinkId; }
    public String getPayosCheckoutUrl() { return payosCheckoutUrl; }
    public void setPayosCheckoutUrl(String payosCheckoutUrl) { this.payosCheckoutUrl = payosCheckoutUrl; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public User getStaff() { return staff; }
    public void setStaff(User staff) { this.staff = staff; }
    public User getShipper() { return shipper; }
    public void setShipper(User shipper) { this.shipper = shipper; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getReadyAt() { return readyAt; }
    public void setReadyAt(LocalDateTime readyAt) { this.readyAt = readyAt; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public String getRefundNote() { return refundNote; }
    public void setRefundNote(String refundNote) { this.refundNote = refundNote; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
