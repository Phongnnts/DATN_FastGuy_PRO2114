package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entity.Orders;
import entity.User;
import org.junit.jupiter.api.Test;

class OrderTransitionServiceTest {
    @Test
    void paidCodCanBeDelivered() {
        assertTrue(OrderTransitionService.canDeliver("COD", "PAID"));
    }

    @Test
    void unpaidOrderCannotBeDelivered() {
        assertFalse(OrderTransitionService.canDeliver("COD", "UNPAID"));
        assertFalse(OrderTransitionService.canDeliver("BANK_TRANSFER", "UNPAID"));
    }

    @Test
    void adminCancellationCanUseGenericTransitionFromCancellableState() {
        OrderTransitionService service = new OrderTransitionService();
        assertTrue(service.canTransition("PENDING", "CANCELLED"));
        assertTrue(OrderTransitionService.canUseGenericTransition("CANCELLED"));
        assertFalse(OrderTransitionService.canUseGenericTransition("ASSIGNED"));
        assertTrue(OrderTransitionService.canUseGenericTransition("CONFIRMED"));
    }

    @Test
    void waitingStockConfirmationIsNotAWorkflowState() {
        OrderTransitionService service = new OrderTransitionService();
        assertFalse(service.canTransition("WAITING_STOCK_CONFIRM", "PENDING"));
        assertFalse(service.getAllowedActions("WAITING_STOCK_CONFIRM", "STAFF", "UNPAID").contains("PENDING"));
    }

    @Test
    void onlyCanonicalStatusesAndActorsAreAccepted() {
        assertTrue(OrderTransitionService.isCanonicalStatus("CANCELLED"));
        assertFalse(OrderTransitionService.isCanonicalStatus("WAITING_STOCK_CONFIRM"));
        assertTrue(OrderTransitionService.isActorRole("SYSTEM"));
        assertTrue(OrderTransitionService.isActorRole("CUSTOMER"));
        assertFalse(OrderTransitionService.isActorRole("GUEST"));
    }

    @Test
    void customerCancellationRequiresExpectedOwner() {
        Orders order = cancellableOrder();
        User owner = new User();
        owner.setUserId(7);
        order.setUser(owner);
        assertTrue(OrderTransitionService.canCancel(order, 7, null, true, "USER"));
        assertFalse(OrderTransitionService.canCancel(order, 8, null, true, "USER"));
    }

    @Test
    void schedulerCancellationRequiresExpectedUnpaidStatus() {
        Orders order = cancellableOrder();
        assertTrue(OrderTransitionService.canCancel(order, null, "UNPAID", false, "SYSTEM"));
        order.setPaymentStatus("PAID");
        assertFalse(OrderTransitionService.canCancel(order, null, "UNPAID", false, "SYSTEM"));
    }

    private Orders cancellableOrder() {
        Orders order = new Orders();
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        return order;
    }

    @Test
    void assignedOrderCanBePickedUpButReadyOrderCannotSkipAssignment() {
        OrderTransitionService service = new OrderTransitionService();
        assertTrue(service.canTransition("READY", "ASSIGNED"));
        assertTrue(service.canTransition("ASSIGNED", "PICKED_UP"));
        assertFalse(service.canTransition("READY", "PICKED_UP"));
        assertTrue(service.getAllowedActions("READY", "STAFF", "UNPAID").contains("ASSIGNED"));
        assertTrue(service.getAllowedActions("ASSIGNED", "SHIPPER", "UNPAID").contains("PICKED_UP"));
    }

    @Test
    void deliveryFailureSupportsRetryOrTerminalStoreReturn() {
        OrderTransitionService service = new OrderTransitionService();
        assertTrue(service.canTransition("PICKED_UP", "DELIVERY_FAILED"));
        assertTrue(service.canTransition("DELIVERY_FAILED", "PICKED_UP"));
        assertTrue(service.canTransition("DELIVERY_FAILED", "RETURNED_TO_STORE"));
        assertFalse(service.canTransition("RETURNED_TO_STORE", "PICKED_UP"));
        assertTrue(OrderTransitionService.isCanonicalStatus("DELIVERY_FAILED"));
        assertTrue(OrderTransitionService.isCanonicalStatus("RETURNED_TO_STORE"));
    }

    @Test
    void recoveryAndReturnCannotUseGenericTransition() {
        assertFalse(OrderTransitionService.canUseGenericTransition("PICKED_UP"));
        assertFalse(OrderTransitionService.canUseGenericTransition("RETURNED_TO_STORE"));
        assertTrue(OrderTransitionService.canUseGenericTransition("DELIVERY_FAILED"));
    }

    @Test
    void everyGenericOverloadRejectsDedicatedRecoveryBeforeDatabaseAccess() {
        OrderTransitionService service = new OrderTransitionService();
        assertFalse(service.transition(1, "PICKED_UP", "STAFF", Integer.valueOf(2), null, null, null));
        assertTrue(service.transition(1, "RETURNED_TO_STORE", "SHIPPER", Integer.valueOf(2), null,
                null, null, "DELIVERY_FAILED") == OrderTransitionService.MutationResult.INVALID);
    }

    @Test
    void pickedUpOrderCannotBeCancelled() {
        OrderTransitionService service = new OrderTransitionService();
        assertFalse(service.canTransition("PICKED_UP", "CANCELLED"));
        assertFalse(service.getAllowedActions("PICKED_UP", "SHIPPER", "PAID").contains("CANCELLED"));
    }

    @Test
    void returnedOrderCannotBeCancelled() {
        Orders order = cancellableOrder();
        order.setOrderStatus("RETURNED_TO_STORE");
        assertFalse(OrderTransitionService.canCancel(order, null, null, false, "STAFF"));
    }
}
