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
  getDeliveryZones() {
    return client.get('/admin/delivery-zones');
  },
  createDeliveryZone(data) {
    return client.post('/admin/delivery-zones', data);
  },
  updateDeliveryZone(id, data) {
    return client.put(`/admin/delivery-zones/${id}`, data);
  },
  deleteDeliveryZone(id) {
    return client.delete(`/admin/delivery-zones/${id}`);
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
};
