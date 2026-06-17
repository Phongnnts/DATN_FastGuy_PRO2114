package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private int paymentId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders order;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "status")
    private String status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "shipper_id")
    private User shipper;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    public Payment() {}

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public User getShipper() { return shipper; }
    public void setShipper(User shipper) { this.shipper = shipper; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
}
