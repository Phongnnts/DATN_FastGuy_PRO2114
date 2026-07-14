import client from './client';

export default {
  getDashboard() {
    return client.get('/admin/dashboard');
  },
  getUsers(params) {
    return client.get('/admin/users', { params });
  },
  createUser(data) {
    return client.post('/admin/users', data);
  },
  updateUser(id, data) {
    return client.put(`/admin/users/${id}`, data);
  },
  deleteUser(id) {
    return client.delete(`/admin/users/${id}`);
  },
  getProducts(params) {
    return client.get('/admin/products', { params });
  },
  createProduct(data) {
    return client.post('/admin/products', data);
  },
  updateProduct(id, data) {
    return client.put(`/admin/products/${id}`, data);
  },
  deleteProduct(id) {
    return client.delete(`/admin/products/${id}`);
  },
  getCategories() {
    return client.get('/admin/categories');
  },
  createCategory(data) {
    return client.post('/admin/categories', data);
  },
  updateCategory(id, data) {
    return client.put(`/admin/categories/${id}`, data);
  },
  deleteCategory(id) {
    return client.delete(`/admin/categories/${id}`);
  },
  getOrders(params) {
    return client.get('/admin/orders', { params });
  },
  getRevenueReport(params) {
    return client.get('/admin/reports/revenue', { params });
  },
  getTopProducts(params) {
    return client.get('/admin/reports/top-products', { params });
  },
  exportReport(type, params) {
    return client.get(`/admin/reports/export/${type}`, {
      params,
      responseType: 'blob',
    });
  },
  getVariants(productId) {
    return client.get(`/admin/products/${productId}/variants`);
  },
  createVariant(productId, data) {
    return client.post(`/admin/products/${productId}/variants`, data);
  },
  updateVariant(id, data) {
    return client.put(`/admin/variants/${id}`, data);
  },
  deleteVariant(id) {
    return client.delete(`/admin/variants/${id}`);
  },
  getModifierGroups(productId) {
    return client.get(`/admin/products/${productId}/modifier-groups`);
  },
  createModifierGroup(productId, data) {
    return client.post(`/admin/products/${productId}/modifier-groups`, data);
  },
  createModifierOption(groupId, data) {
    return client.post(`/admin/products/${groupId}/modifier-groups/options`, data);
  },
  getCombo(productId) {
    return client.get(`/admin/products/${productId}/combo`);
  },
  saveCombo(productId, data) {
    return client.post(`/admin/products/${productId}/combo`, data);
  },
  createComboItem(productId, data) {
    return client.post(`/admin/products/${productId}/combo/items`, data);
  },
  getSettings() {
    return client.get('/admin/settings');
  },
  updateSettings(data) {
    return client.put('/admin/settings', data);
  },
  getShifts() {
    return client.get('/admin/shifts');
  },
  createShift(data) {
    return client.post('/admin/shifts', data);
  },
  updateShift(id, data) {
    return client.put(`/admin/shifts/${id}`, data);
  },
  deleteShift(id) {
    return client.delete(`/admin/shifts/${id}`);
  },
  updateRefund(orderId, data) {
    return client.put(`/admin/refunds/${orderId}`, data);
  },
};
