import client from './client';

export default {
  getAll(params) {
    return client.get('/products', { params });
  },
  getById(id) {
    return client.get(`/products/${id}`);
  },
  getByCategory(categoryId) {
    return client.get('/products', { params: { categoryId } });
  },
  search(query) {
    return client.get('/products', { params: { q: query } });
  },
  getCategories() {
    return client.get('/categories');
  },
  getFeatured() {
    return client.get('/products/featured');
  },
  create(data) {
    return client.post('/products', data);
  },
  update(id, data) {
    return client.put(`/products/${id}`, data);
  },
  delete(id) {
    return client.delete(`/products/${id}`);
  },
};
