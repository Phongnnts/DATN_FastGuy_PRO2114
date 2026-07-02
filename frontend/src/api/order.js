import client from './client';

export default {
  getAll(params) {
    return client.get('/orders', { params });
  },
  getById(id) {
    return client.get(`/orders/${id}`);
  },
  create(data) {
    return client.post('/orders', data);
  },
  cancel(id) {
    return client.put(`/orders/${id}/cancel`);
  },
  trackOrder(orderCode) {
    return client.get('/orders/track', { params: { code: orderCode } });
  },
  getHistory(params) {
    return client.get('/orders/history', { params });
  },
  getPaymentStatus(id) {
    return client.get(`/orders/${id}/payment-status`);
  },
  confirmPayment(id) {
    return client.put(`/orders/${id}/confirm-payment`);
  },
  guestCheckout(data) {
    return client.post('/orders/guest-checkout', data);
  },
};
