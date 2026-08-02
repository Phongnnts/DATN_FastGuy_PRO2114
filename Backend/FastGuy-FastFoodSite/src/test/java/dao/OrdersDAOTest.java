package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.User;
import entity.Product;
import entity.ProductVariant;

class OrdersDAOTest {

    private Orders createOrder(String status, String paymentStatus, BigDecimal amount, LocalDateTime deliveredAt) {
        Orders o = new Orders();
        o.setOrderStatus(status);
        o.setPaymentStatus(paymentStatus);
        o.setFinalAmount(amount);
        o.setDeliveredAt(deliveredAt);
        o.setOrderCode("TEST-" + System.nanoTime());
        o.setCustomerName("Test");
        o.setCustomerPhone("0900000000");
        o.setCustomerAddress("123 Test");
        o.setTotalAmount(amount);
        o.setPaymentMethod("COD");
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    @Test
    @DisplayName("Order entity has correct getters/setters for status fields")
    void orderStatusFields() {
        Orders o = new Orders();
        o.setOrderStatus("PENDING");
        o.setPaymentStatus("UNPAID");
        assertEquals("PENDING", o.getOrderStatus());
        assertEquals("UNPAID", o.getPaymentStatus());
    }

    @Test
    @DisplayName("Order entity has correct amount fields")
    void orderAmountFields() {
        Orders o = new Orders();
        o.setTotalAmount(new BigDecimal("100000"));
        o.setShippingFee(new BigDecimal("15000"));
        o.setServiceFee(new BigDecimal("5000"));
        o.setDiscountAmount(new BigDecimal("10000"));
        o.setFinalAmount(new BigDecimal("110000"));
        assertEquals(new BigDecimal("110000"), o.getFinalAmount());
    }

    @Test
    @DisplayName("Order entity has correct GHN fields as Transient")
    void orderTransientFields() {
        Orders o = new Orders();
        o.setGhnOrderCode("GHN123");
        o.setGhnTrackingUrl("https://track.ghn.vn/123");
        o.setShippingServiceId(2);
        o.setShippingServiceTypeId(1);
        assertEquals("GHN123", o.getGhnOrderCode());
        assertEquals("https://track.ghn.vn/123", o.getGhnTrackingUrl());
    }

    @Test
    @DisplayName("Order timestamp fields work correctly")
    void orderTimestamps() {
        Orders o = new Orders();
        LocalDateTime now = LocalDateTime.now();
        o.setConfirmedAt(now);
        o.setReadyAt(now.plusMinutes(10));
        o.setPickedUpAt(now.plusMinutes(20));
        o.setDeliveredAt(now.plusMinutes(30));
        o.setCancelledAt(null);
        assertEquals(now, o.getConfirmedAt());
        assertEquals(now.plusMinutes(30), o.getDeliveredAt());
    }

    @Test
    @DisplayName("Order user/shipper/staff relationships")
    void orderRelationships() {
        Orders o = new Orders();
        User staff = new User();
        staff.setUserId(10);
        User shipper = new User();
        shipper.setUserId(20);
        o.setStaff(staff);
        o.setShipper(shipper);
        assertEquals(10, o.getStaff().getUserId());
        assertEquals(20, o.getShipper().getUserId());
    }
}
