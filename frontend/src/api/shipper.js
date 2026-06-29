import client from './client';

export default {
  getDashboard() {
    return client.get('/shipper/dashboard');
  },
  getAvailableOrders() {
    return client.get('/shipper/orders');
  },
  getMyOrders() {
    return client.get('/shipper/orders/mine');
  },
  getActiveOrders() {
    return client.get('/shipper/orders/active');
  },
  getHistory() {
    return client.get('/shipper/orders/history');
  },
  getOrderById(id) {
    return client.get(`/shipper/orders/${id}`);
  },
  pickUpOrder(id) {
    return client.put(`/shipper/orders/${id}/pickup`);
  },
  deliverOrder(id) {
    return client.put(`/shipper/orders/${id}/deliver`);
  },
  cancelOrder(id, reason) {
    return client.put(`/shipper/orders/${id}/cancel`, { reason });
  },
};
