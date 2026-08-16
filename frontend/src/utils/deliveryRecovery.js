export const CUSTOMER_DELIVERY_FAILURE_MESSAGE = 'Giao chưa thành công, cửa hàng đang xử lý';

export function customerDeliveryStatus(order) {
  return order?.status === 'DELIVERY_FAILED'
    ? { message: CUSTOMER_DELIVERY_FAILURE_MESSAGE, retryScheduledAt: order.retryScheduledAt || null }
    : null;
}

export function deliveryRetryMode(reasonCode) {
  return reasonCode === 'CUSTOMER_RESCHEDULED' ? 'SCHEDULED' : 'IMMEDIATE';
}

export function isCompletedRevenue(order) {
  return order?.status === 'DELIVERED' && order?.paymentStatus === 'PAID';
}
