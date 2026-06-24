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
  assignOrder(id, staffId) {
    return client.put(`/staff/orders/${id}/assign`, { staffId });
  },
  saveInternalNote(id, note) {
    return client.post(`/staff/orders/${id}/notes`, { note });
  },
  getIngredients() {
    return client.get('/staff/ingredients');
  },
  getLowStock() {
    return client.get('/staff/ingredients/low-stock');
  },
  stockIn(id, data) {
    return client.post(`/staff/ingredients/${id}/stock-in`, data);
  },
  checkIn() {
    return client.post('/staff/shifts/check-in');
  },
  checkOut() {
    return client.post('/staff/shifts/check-out');
  },
  getShifts() {
    return client.get('/staff/shifts');
  },
  getOrderHistory(params) {
    return client.get('/staff/orders/history', { params });
  },
  exportOrders(params) {
    return client.get('/staff/orders/export', { params, responseType: 'blob' });
  },
};
