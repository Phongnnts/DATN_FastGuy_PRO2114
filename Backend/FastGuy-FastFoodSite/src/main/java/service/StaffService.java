package service;

import dto.OrderDTO;
import dto.OrderItemDTO;
import entity.*;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class StaffService {
    private final OrderRepository orderRepository = new OrderRepository();
    private final IngredientRepository ingredientRepository = new IngredientRepository();
    private final InventoryTransactionRepository transactionRepository = new InventoryTransactionRepository();
    private final UserRepository userRepository = new UserRepository();

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO confirmOrder(Long orderId, Long staffId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng không ở trạng thái chờ xác nhận");
        }
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("User", staffId));
        order.setOrderStatus("CONFIRMED");
        order.setStaff(staff);
        order.setConfirmedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public OrderDTO markReady(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!"CONFIRMED".equals(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng chưa được xác nhận");
        }
        order.setOrderStatus("READY");
        order.setReadyAt(LocalDateTime.now());
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public OrderDTO cancelOrder(Long orderId, String reason) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!List.of("PENDING", "CONFIRMED").contains(order.getOrderStatus())) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái hiện tại");
        }
        order.setOrderStatus("CANCELLED");
        order.setFailureReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public List<entity.Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }

    public List<OrderDTO> getOrderHistory(Long staffId) {
        return orderRepository.findByStaffId(staffId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO updateInternalNote(Long orderId, String note) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setInternalNote(note);
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public List<entity.Ingredient> getLowStockIngredients() {
        return ingredientRepository.findLowStock();
    }

    public void stockIn(Long ingredientId, BigDecimal quantity, Long staffId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient", ingredientId));
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("User", staffId));

        ingredient.setStockQuantity(ingredient.getStockQuantity().add(quantity));
        ingredientRepository.save(ingredient);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setIngredient(ingredient);
        tx.setQuantityChange(quantity);
        tx.setReason("STOCK_IN");
        tx.setCreatedBy(staff);
        transactionRepository.save(tx);
    }

    private OrderDTO toDTO(Orders order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderCode(order.getOrderCode());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerAddress(order.getCustomerAddress());
        dto.setZoneId(order.getZone().getZoneId());
        dto.setZoneName(order.getZone().getDistrictName());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setInternalNote(order.getInternalNote());
        dto.setDeliveryNote(order.getDeliveryNote());
        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(item -> {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setOrderItemId(item.getOrderItemId());
                itemDTO.setProductId(item.getProduct() != null ? item.getProduct().getProductId() : null);
                itemDTO.setProductName(item.getProductName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setOptionData(item.getOptionData());
                itemDTO.setTotalPrice(item.getTotalPrice());
                return itemDTO;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
