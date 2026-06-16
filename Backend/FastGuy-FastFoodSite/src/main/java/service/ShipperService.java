package service;

import dto.OrderDTO;
import dto.OrderItemDTO;
import entity.Orders;
import entity.Payment;
import entity.User;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import repository.OrderRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ShipperService {
    private final OrderRepository orderRepository = new OrderRepository();
    private final UserRepository userRepository = new UserRepository();

    public List<OrderDTO> getDeliveries(Long shipperId) {
        return orderRepository.findByShipperId(shipperId).stream()
                .filter(o -> List.of("READY", "DELIVERING").contains(o.getOrderStatus()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getDeliveryHistory(Long shipperId) {
        return orderRepository.findByShipperId(shipperId).stream()
                .filter(o -> List.of("DELIVERED", "FAILED").contains(o.getOrderStatus()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO updateDeliveryNote(Long orderId, String deliveryNote) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setDeliveryNote(deliveryNote);
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public OrderDTO pickUp(Long orderId, Long shipperId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!"READY".equals(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng chưa sẵn sàng để giao");
        }
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", shipperId));
        order.setOrderStatus("DELIVERING");
        order.setShipper(shipper);
        order.setPickedUpAt(LocalDateTime.now());
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public OrderDTO deliver(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!"DELIVERING".equals(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng không ở trạng thái đang giao");
        }
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now());
        if ("COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
        }
        order = orderRepository.save(order);

        Payment payment = order.getPayment();
        if (payment != null && "COD".equals(order.getPaymentMethod())) {
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
        }
        orderRepository.save(order);
        return toDTO(order);
    }

    public OrderDTO failDelivery(Long orderId, String reason) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!"DELIVERING".equals(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng không ở trạng thái đang giao");
        }
        order.setOrderStatus("FAILED");
        order.setFailureReason(reason);
        order = orderRepository.save(order);
        return toDTO(order);
    }

    public OrderDTO collectPayment(Long orderId, Long shipperId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new BadRequestException("Đơn hàng chưa được giao");
        }
        Payment payment = order.getPayment();
        if (payment == null) {
            throw new BadRequestException("Không có thông tin thanh toán");
        }
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", shipperId));
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        payment.setShipper(shipper);
        payment.setCollectedAt(LocalDateTime.now());
        order.setPaymentStatus("PAID");
        orderRepository.save(order);
        return toDTO(order);
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
