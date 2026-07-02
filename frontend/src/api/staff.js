import client from './client';

export default {
  getDashboard() {
    return client.get('/staff/dashboard');
  },
  getOrders(params) {
    return client.get('/staff/orders', { params });
  },
  getConfirmedOrders(params) {
    return client.get('/staff/orders/confirmed', { params });
  },
  getPreparingOrders(params) {
    return client.get('/staff/orders/preparing', { params });
  },
  getReadyOrders(params) {
    return client.get('/staff/orders/ready', { params });
  },
  getOrderById(id) {
    return client.get(`/staff/orders/${id}`);
  },
  updateOrderStatus(id, status, failureReason) {
    return client.put(`/staff/orders/${id}/status`, { status, failureReason });
  },
  assignShipper(id, shipperId) {
    return client.put(`/staff/orders/${id}/assign-shipper`, { shipperId });
  },
  getAvailableShippers() {
    return client.get('/staff/orders/shippers');
  },
  saveInternalNote(id, note) {
    return client.post(`/staff/orders/${id}/notes`, { note });
  },
  getOrderHistory(params) {
    return client.get('/staff/orders/history', { params });
  },
  exportOrders(params) {
    return client.put('/staff/orders/export', null, { params, responseType: 'blob' });
  },
};
