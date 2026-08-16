package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Transient;

class OrdersMappingTest {
    @Test
    void shippingMetadataIsPersisted() throws Exception {
        Map<String, String> columns = Map.of(
                "shippingServiceId", "shipping_service_id",
                "shippingServiceTypeId", "shipping_service_type_id",
                "expectedDeliveryTime", "expected_delivery_time",
                "ghnOrderCode", "ghn_order_code",
                "ghnTrackingUrl", "ghn_tracking_url",
                "ghnStatus", "ghn_status",
                "fromDistrictId", "from_district_id",
                "fromWardCode", "from_ward_code");

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            Field field = Orders.class.getDeclaredField(entry.getKey());
            assertFalse(field.isAnnotationPresent(Transient.class), entry.getKey());
            assertEquals(entry.getValue(), field.getAnnotation(Column.class).name());
        }
    }

    @Test
    void deliveryFailureRecoveryMetadataIsPersisted() throws Exception {
        Map<String, String> columns = Map.of(
                "deliveryAttemptCount", "delivery_attempt_count",
                "deliveryAttemptLimit", "delivery_attempt_limit",
                "deliveryFailureCode", "delivery_failure_code",
                "deliveryFailedAt", "delivery_failed_at",
                "retryScheduledAt", "retry_scheduled_at",
                "returnedToStoreAt", "returned_to_store_at");

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            Field field = Orders.class.getDeclaredField(entry.getKey());
            assertFalse(field.isAnnotationPresent(Transient.class), entry.getKey());
            assertEquals(entry.getValue(), field.getAnnotation(Column.class).name());
        }

        Orders order = new Orders();
        assertEquals(0, order.getDeliveryAttemptCount());
        assertEquals(2, order.getDeliveryAttemptLimit());
    }

    @Test
    void refundAuditMetadataIsPersisted() throws Exception {
        Map<String, String> columns = Map.of(
                "refundProcessedBy", "refund_processed_by",
                "refundReference", "refund_reference");

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            Field field = Orders.class.getDeclaredField(entry.getKey());
            assertFalse(field.isAnnotationPresent(Transient.class), entry.getKey());
            assertEquals(entry.getValue(), field.getAnnotation(Column.class).name());
        }
    }
}
