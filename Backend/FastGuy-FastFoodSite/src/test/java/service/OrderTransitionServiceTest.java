package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entity.Orders;
import entity.User;
import entity.WorkShift;
import java.time.LocalDateTime;
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
    void assignmentRequiresCheckedInStaffIdentityAndExactReadyExpectation() {
        for (String role : new String[] { "SHIPPER", "CUSTOMER", "SYSTEM", "ADMIN" }) {
            assertFalse(OrderTransitionService.canAssignOrder(role, 7, "READY", true));
        }
        assertFalse(OrderTransitionService.canAssignOrder("STAFF", null, "READY", true));
        assertFalse(OrderTransitionService.canAssignOrder("STAFF", 7, null, true));
        assertFalse(OrderTransitionService.canAssignOrder("STAFF", 7, "ASSIGNED", true));
        assertFalse(OrderTransitionService.canAssignOrder("STAFF", 7, "READY", false));
        assertTrue(OrderTransitionService.canAssignOrder("STAFF", 7, "READY", true));
    }

    @Test
    void ownershipLifecycleSetsPreservesAndClearsShift() {
        WorkShift shift = new WorkShift();
        Orders order = new Orders();
        OrderTransitionService.applyStaffShiftOwnership(order, "CONFIRMED", shift);
        assertTrue(order.getStaffShift() == shift);
        OrderTransitionService.applyStaffShiftOwnership(order, "PREPARING", null);
        assertTrue(order.getStaffShift() == shift);
        OrderTransitionService.applyStaffShiftOwnership(order, "ASSIGNED", null);
        assertTrue(order.getStaffShift() == null);
        order.setStaffShift(shift);
        OrderTransitionService.applyStaffShiftOwnership(order, "DELIVERY_FAILED", null);
        assertTrue(order.getStaffShift() == null);
    }

    @Test
    void claimRequiresEligibleStatusDifferentOwnerAndExpectedValues() {
        WorkShift current = new WorkShift(); current.setShiftId(9);
        WorkShift other = new WorkShift(); other.setShiftId(8);
        Orders order = new Orders(); order.setOrderStatus("PREPARING"); order.setStaffShift(other);
        assertTrue(OrderTransitionService.canClaimHandover(order, current, "PREPARING", 8));
        assertFalse(OrderTransitionService.canClaimHandover(order, current, "READY", 8));
        assertFalse(OrderTransitionService.canClaimHandover(order, current, "PREPARING", null));
        order.setStaffShift(current);
        assertFalse(OrderTransitionService.canClaimHandover(order, current, "PREPARING", 9));
    }

    @Test
    void adminConfirmationSucceedsWithoutAssigningStaffShiftWhileStaffRequiresShift() {
        Orders adminOrder = new Orders(); adminOrder.setOrderStatus("PENDING");
        assertTrue(OrderTransitionService.canActorConfirm("ADMIN", null));
        OrderTransitionService.applyActorOwnership(adminOrder, "CONFIRMED", "ADMIN", null);
        assertTrue(adminOrder.getStaffShift() == null);
        assertFalse(OrderTransitionService.canActorConfirm("STAFF", null));
        WorkShift shift = new WorkShift(); shift.setShiftId(9);
        assertTrue(OrderTransitionService.canActorConfirm("STAFF", shift));
    }

    @Test
    void staffMutationRequiresCurrentShiftOwnershipExceptConfirmation() {
        WorkShift current = new WorkShift(); current.setShiftId(9);
        WorkShift other = new WorkShift(); other.setShiftId(8);
        Orders order = new Orders(); order.setOrderStatus("CONFIRMED"); order.setStaffShift(other);
        assertFalse(OrderTransitionService.canStaffMutateOwnedOrder(order, current, "PREPARING"));
        order.setStaffShift(current);
        assertTrue(OrderTransitionService.canStaffMutateOwnedOrder(order, current, "PREPARING"));
        order.setOrderStatus("PENDING"); order.setStaffShift(null);
        assertTrue(OrderTransitionService.canStaffMutateOwnedOrder(order, current, "CONFIRMED"));
    }

    @Test
    void deliveryFailureRecoveryRequiresActingShiftOwnership() {
        WorkShift current = new WorkShift(); current.setShiftId(9);
        WorkShift other = new WorkShift(); other.setShiftId(8);
        Orders order = new Orders(); order.setOrderStatus("DELIVERY_FAILED"); order.setStaffShift(other);
        assertFalse(OrderTransitionService.isOwnedBy(order, current));
        order.setStaffShift(current);
        assertTrue(OrderTransitionService.isOwnedBy(order, current));
    }

    @Test
    void retryAssignmentSetsAssignedAt() {
        Orders order = new Orders();
        User shipper = new User();
        OrderTransitionService.assignRetryShipper(order, shipper, LocalDateTime.of(2026, 8, 27, 16, 0));
        assertTrue(order.getShipper() == shipper);
        assertTrue(order.getAssignedAt() != null);
    }

    @Test
    void immediateRetryClearsStaffShift() {
        Orders order = ownedDeliveryFailure();
        OrderTransitionService.clearOwnershipAfterRecovery(order, "PICKED_UP");
        assertTrue(order.getStaffShift() == null);
    }

    @Test
    void scheduledRetryStartClearsStaffShift() {
        Orders order = ownedDeliveryFailure();
        OrderTransitionService.clearOwnershipAfterRecovery(order, "PICKED_UP");
        assertTrue(order.getStaffShift() == null);
    }

    @Test
    void returnToStoreClearsStaffShift() {
        Orders order = ownedDeliveryFailure();
        OrderTransitionService.clearOwnershipAfterRecovery(order, "RETURNED_TO_STORE");
        assertTrue(order.getStaffShift() == null);
    }

    private Orders ownedDeliveryFailure() {
        WorkShift shift = new WorkShift(); shift.setShiftId(9);
        Orders order = new Orders(); order.setOrderStatus("DELIVERY_FAILED"); order.setStaffShift(shift);
        return order;
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
