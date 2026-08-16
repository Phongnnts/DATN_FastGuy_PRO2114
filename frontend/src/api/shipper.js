import client from './client';

export default {
  getDashboard() {
    return client.get('/shipper/dashboard');
  },
  getMyOrders() {
    return client.get('/shipper/orders/mine');
  },
  getActiveOrders() {
    return client.get('/shipper/orders/active');
  },
  getHistory(params) {
    return client.get('/shipper/orders/history', { params });
  },
  getOrderById(id) {
    return client.get(`/shipper/orders/${id}`);
  },
  pickUpOrder(id, expectedStatus) {
    return client.put(`/shipper/orders/${id}/pickup`, { expectedStatus });
  },
  deliverOrder(id, collectedAmount, expectedStatus) {
    return client.put(`/shipper/orders/${id}/deliver`, collectedAmount === undefined ? { expectedStatus } : { collectedAmount, expectedStatus });
  },
  failOrder(orderId, { expectedStatus, reasonCode, note }) {
    return client.post(`/shipper/orders/${orderId}/fail`, { expectedStatus, reasonCode, note });
  },
};
