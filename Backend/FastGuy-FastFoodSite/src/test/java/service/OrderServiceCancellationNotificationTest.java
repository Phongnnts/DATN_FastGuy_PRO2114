package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderServiceCancellationNotificationTest {
    @Test
    void systemCancellationUsesAutomaticCancellationCopy() {
        assertEquals("Đơn hàng tự động hủy", OrderService.cancellationTitle("SYSTEM"));
        assertEquals("Khách hủy đơn", OrderService.cancellationTitle("USER"));
    }
}
