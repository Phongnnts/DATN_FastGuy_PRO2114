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
  review(id, data) {
    return client.post(`/orders/${id}/review`, data);
  },
  getHistory(params) {
    return client.get('/orders/history', { params });
  },
  getPaymentStatus(id) {
    return client.get(`/orders/${id}/payment-status`);
  },
  getSePayQrUrl(amount, orderCode) {
    const description = `TT ${orderCode}`;
    return `https://qr.sepay.vn/img?acc=6513527&bank=MB&amount=${amount}&des=${encodeURIComponent(description)}&template=compact`;
  },
};
