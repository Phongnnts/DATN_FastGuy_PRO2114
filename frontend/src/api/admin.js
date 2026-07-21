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
  getOrderById(id) {
    return client.get(`/admin/orders/${id}`);
  },
  getFullReport(params) {
    return client.get('/admin/reports/full', { params });
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
  updateModifierGroup(groupId, data) {
    return client.put(`/admin/products/${groupId}/modifier-groups`, data);
  },
  deleteModifierGroup(groupId) {
    return client.delete(`/admin/products/modifier-groups/${groupId}`);
  },
  updateModifierOption(groupId, optionId, data) {
    return client.put(`/admin/products/${groupId}/modifier-groups/options/${optionId}`, data);
  },
  deleteModifierOption(groupId, optionId) {
    return client.delete(`/admin/products/${groupId}/modifier-groups/options/${optionId}`);
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
  updateCombo(productId, data) {
    return client.put(`/admin/products/${productId}/combo`, data);
  },
  deleteComboItem(productId, itemId) {
    return client.delete(`/admin/products/${productId}/combo/items/${itemId}`);
  },
  getSettings() {
    return client.get('/admin/settings');
  },
  updateSettings(data) {
    return client.put('/admin/settings', data);
  },
  getShifts(params) {
    return client.get('/admin/shifts', { params });
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
  getRefunds() {
    return client.get('/admin/refunds');
  },
  cancelOrder(orderId, data) {
    return client.put(`/admin/orders/${orderId}/cancel`, data || {});
  },
  updateOrderStatus(orderId, data) {
    return client.put(`/admin/orders/${orderId}/status`, data);
  },
  addOrderNote(orderId, note) {
    return client.post(`/admin/orders/${orderId}/notes`, { note });
  },
  getUserOrders(userId) {
    return client.get(`/admin/users/${userId}/orders`);
  },
  updateUserStatus(userId, data) {
    return client.put(`/admin/users/${userId}/status`, data);
  },
};
