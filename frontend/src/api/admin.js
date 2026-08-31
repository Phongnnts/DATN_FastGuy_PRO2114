import client from './client';

export default {
  getDashboard() {
    return client.get('/admin/dashboard');
  },
  getActivityLogs(params) {
    return client.get('/admin/activity-logs', { params });
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
  getProduct(id) {
    return client.get(`/admin/products/${id}`);
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
  restoreProduct(id) {
    return client.put(`/admin/products/${id}/restore`, {});
  },
  permanentlyDeleteProduct(id) {
    return client.delete(`/admin/products/${id}/permanent`);
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
  getOperatingExpenses(params) { return client.get('/admin/operating-expenses', { params }); },
  createOperatingExpense(data) { return client.post('/admin/operating-expenses', data); },
  updateOperatingExpense(id, data) { return client.put(`/admin/operating-expenses/${id}`, data); },
  deleteOperatingExpense(id) { return client.delete(`/admin/operating-expenses/${id}`); },
  getFixedAssets() { return client.get('/admin/fixed-assets'); },
  createFixedAsset(data) { return client.post('/admin/fixed-assets', data); },
  updateFixedAsset(id, data) { return client.put(`/admin/fixed-assets/${id}`, data); },
  retireFixedAsset(id) { return client.put(`/admin/fixed-assets/${id}/retire`, { expectedStatus: 'ACTIVE' }); },
  getOperatingProfitReport(params) { return client.get('/admin/reports/operating-profit', { params }); },
  getInventoryTransactions(params) {
    return client.get('/admin/inventory/transactions', { params });
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
  getSettings() {
    return client.get('/admin/settings');
  },
  updateSettings(data) {
    return client.put('/admin/settings', data);
  },
  getShifts(params) {
    return client.get('/admin/shifts', { params });
  },
  getShiftWeek(weekStart) {
    return client.get('/admin/shifts/week', { params: { weekStart } });
  },
  replaceShiftWeek(data) {
    return client.put('/admin/shifts/week', data);
  },
  getShiftMonitoring() {
    return client.get('/admin/shifts/monitoring');
  },
  getShiftAttendance(params) {
    return client.get('/admin/shifts/attendance', { params });
  },
  approveShiftAttendance(id, data) {
    return client.put(`/admin/shifts/${id}/attendance-approval`, data);
  },
  getStaffPayRates(userId) { return client.get(`/admin/staff/${userId}/pay-rates`); },
  createStaffPayRate(userId, data) { return client.post(`/admin/staff/${userId}/pay-rates`, data); },
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
  getRefunds(params) {
    return client.get('/admin/refunds', { params });
  },
  cancelOrder(orderId, data) {
    return client.put(`/admin/orders/${orderId}/cancel`, data || {});
  },
  updateOrderStatus(orderId, data) {
    return client.put(`/admin/orders/${orderId}/status`, data);
  },
  addOrderNote(orderId, expectedStatus, note) {
    return client.post(`/admin/orders/${orderId}/notes`, { expectedStatus, note });
  },
  updateFeaturedReview(orderId, featured) {
    return client.put(`/admin/orders/${orderId}/featured-review`, { featured });
  },
  overrideDeliveryAttempt(id, expectedStatus, note) {
    return client.post(`/admin/orders/${id}/delivery-attempt-override`, { expectedStatus, note });
  },
  getUserOrders(userId) {
    return client.get(`/admin/users/${userId}/orders`);
  },
  updateUserStatus(userId, data) {
    return client.put(`/admin/users/${userId}/status`, data);
  },
  getInventoryItems() {
    return client.get('/admin/inventory/items');
  },
  createInventoryItem(data) {
    return client.post('/admin/inventory/items', data);
  },
  updateInventoryItem(id, data) {
    return client.put(`/admin/inventory/items/${id}`, data);
  },
  adjustInventoryItem(data) {
    return client.post('/admin/inventory/transactions/adjustments', data);
  },
  getGoodsReceipts() {
    return client.get('/admin/inventory/receipts');
  },
  getGoodsReceipt(id) {
    return client.get(`/admin/inventory/receipts/${id}`);
  },
  createGoodsReceipt(data) {
    return client.post('/admin/inventory/receipts', data);
  },
  updateGoodsReceipt(id, data) {
    return client.put(`/admin/inventory/receipts/${id}`, data);
  },
  deleteGoodsReceipt(id) {
    return client.delete(`/admin/inventory/receipts/${id}`);
  },
  approveGoodsReceipt(id) {
    return client.post(`/admin/inventory/receipts/${id}/approve`);
  },
  getStockCounts() {
    return client.get('/admin/inventory/stock-counts');
  },
  createStockCount(data) {
    return client.post('/admin/inventory/stock-counts', data);
  },
  getStockCount(id) {
    return client.get(`/admin/inventory/stock-counts/${id}`);
  },
  updateStockCount(id, data) {
    return client.put(`/admin/inventory/stock-counts/${id}`, data);
  },
  approveStockCount(id) {
    return client.post(`/admin/inventory/stock-counts/${id}/approve`);
  },
  getInventoryCostSummary(params) {
    return client.get('/admin/inventory/reports/summary', { params });
  },
  getInventoryItemLoss(params) {
    return client.get('/admin/inventory/reports/item-loss', { params });
  },
  getInventoryMenuCost() {
    return client.get('/admin/inventory/reports/menu-cost');
  },
  getMenuPerformanceReport(params) {
    return client.get('/admin/inventory/reports/menu-performance', { params });
  },
  getVariantRecipe(variantId) {
    return client.get(`/admin/product-variants/${variantId}/recipe`);
  },
  replaceVariantRecipe(variantId, data) {
    return client.put(`/admin/product-variants/${variantId}/recipe`, data);
  },
  getVariantInventorySettings(variantId) {
    return client.get(`/admin/product-variants/${variantId}/inventory-settings`);
  },
  updateVariantInventorySettings(variantId, data) {
    return client.put(`/admin/product-variants/${variantId}/inventory-settings`, data);
  },
  getVariantInventoryCapacity(variantId) {
    return client.get(`/admin/product-variants/${variantId}/inventory-capacity`);
  },
  getVariantAvailability(variantId) {
    return client.get(`/admin/product-variants/${variantId}/availability`);
  },
};
