import client from './client';

export default {
  getAll(params) {
    return client.get('/orders', { params });
  },
  getById(id) {
    return client.get(`/orders/${id}`);
  },
  create(data, idempotencyKey) {
    return client.post('/orders', data, { headers: { 'Idempotency-Key': idempotencyKey } });
  },
  cancel(id, data) {
    return client.put(`/orders/${id}/cancel`, data || {});
  },
  trackOrder(orderCode, phoneSuffix) {
    return client.get('/orders/track', { params: { code: orderCode, phoneSuffix }, suppressAuthRedirect: true });
  },
  getHistory(params) {
    return client.get('/orders/history', { params });
  },
  getPaymentStatus(id) {
    return client.get(`/orders/${id}/payment-status`);
  },
  getPaymentCapabilities() {
    return client.get('/orders/payment-capabilities', { suppressAuthRedirect: true });
  },
  getGuestPaymentStatus(orderCode, token) {
    return client.get('/orders/guest-payment-status', { params: { orderCode, token }, suppressAuthRedirect: true });
  },
  guestCheckout(data, idempotencyKey) {
    return client.post('/orders/guest-checkout', data, { headers: { 'Idempotency-Key': idempotencyKey } });
  },
};
