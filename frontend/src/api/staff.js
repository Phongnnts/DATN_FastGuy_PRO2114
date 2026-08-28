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
  getDispatchOrders(filter) {
    return client.get('/staff/orders/dispatch', { params: { filter } });
  },
  getDeliveryFailedOrders(params) {
    return client.get('/staff/orders/delivery-failures', { params });
  },
  getOwnershipCount() {
    return client.get('/staff/orders/ownership-count');
  },
  getOrderById(id) {
    return client.get(`/staff/orders/${id}`);
  },
  updateOrderStatus(id, status, expectedStatus, failureReason) {
    return client.put(`/staff/orders/${id}/status`, { status, expectedStatus, failureReason });
  },
  assignShipper(id, shipperId, expectedStatus) {
    return client.put(`/staff/orders/${id}/assign-shipper`, { shipperId, expectedStatus });
  },
  retryDelivery(id, data) {
    return client.post(`/staff/orders/${id}/retry-delivery`, data);
  },
  startScheduledRetry(id, expectedStatus) {
    return client.post(`/staff/orders/${id}/start-scheduled-retry`, { expectedStatus });
  },
  returnToStore(id, expectedStatus, note) {
    return client.post(`/staff/orders/${id}/return-to-store`, { expectedStatus, note });
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
    return client.get('/staff/orders/export', { params, responseType: 'blob' });
  },
};
