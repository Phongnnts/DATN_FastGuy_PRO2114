package entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "OrderItemModifier")
public class OrderItemModifier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_modifier_id")
    private int orderItemModifierId;

    @ManyToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "modifier_option_id")
    private ProductModifierOption option;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "option_name")
    private String optionName;

    @Column(name = "price")
    private BigDecimal price;

    public int getOrderItemModifierId() { return orderItemModifierId; }
    public void setOrderItemModifierId(int orderItemModifierId) { this.orderItemModifierId = orderItemModifierId; }
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public ProductModifierOption getOption() { return option; }
    public void setOption(ProductModifierOption option) { this.option = option; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
