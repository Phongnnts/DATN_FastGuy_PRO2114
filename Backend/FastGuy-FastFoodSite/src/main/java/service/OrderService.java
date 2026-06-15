package service;

import dto.CreateOrderRequest;
import dto.OrderDTO;
import dto.OrderItemDTO;
import entity.*;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import utils.ValidationUtil;
import repository.AddressRepository;
import repository.CartRepository;
import repository.OrderRepository;
import repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrderService {
    private final OrderRepository orderRepository = new OrderRepository();
    private final CartRepository cartRepository = new CartRepository();
    private final AddressRepository addressRepository = new AddressRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ProductService productService = new ProductService();

    public List<OrderDTO> getByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getById(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toDTO(order);
    }

    public OrderDTO createFromCart(Long userId, CreateOrderRequest req) {
        validateCreateOrder(req);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Giỏ hàng trống"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        Address address = addressRepository.findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", req.getAddressId()));

        Orders order = new Orders();
        order.setOrderCode(orderRepository.generateOrderCode());
        order.setUser(user);
        order.setCustomerName(address.getRecipientName());
        order.setCustomerPhone(address.getPhone());
        order.setCustomerAddress(address.getStreet() + ", " + (address.getWard() != null ? address.getWard() + ", " : "") + address.getCity());
        order.setZone(address.getZone());
        order.setShippingFee(address.getZone().getShippingFee());
        order.setPaymentMethod(req.getPaymentMethod());
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            BigDecimal itemTotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setOptionData(cartItem.getOptionData());
            orderItem.setTotalPrice(itemTotal);
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount.add(order.getShippingFee()));

        order = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setStatus("PENDING");
        if ("COD".equals(req.getPaymentMethod())) {
            payment.setStatus("PENDING");
        }
        order.setPayment(payment);
        order = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return toDTO(order);
    }

    private void validateCreateOrder(CreateOrderRequest req) {
        ValidationUtil.notNull(req.getAddressId(), "Địa chỉ giao hàng");
        ValidationUtil.notBlank(req.getPaymentMethod(), "Phương thức thanh toán");
        ValidationUtil.notNull(req.getItems(), "Sản phẩm");
        if (req.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }
        for (CreateOrderRequest.CreateOrderItem item : req.getItems()) {
            ValidationUtil.notNull(item.getProductId(), "Sản phẩm");
            ValidationUtil.positive(item.getQuantity(), "Số lượng");
        }
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

        if (order.getStaff() != null) dto.setStaffId(order.getStaff().getUserId());
        if (order.getShipper() != null) dto.setShipperId(order.getShipper().getUserId());

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
