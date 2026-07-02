import client from './client';

export default {
  getAll() {
    return client.get('/admin/banners');
  },
  getById(id) {
    return client.get(`/admin/banners/${id}`);
  },
  create(data) {
    return client.post('/admin/banners', data);
  },
  update(id, data) {
    return client.put(`/admin/banners/${id}`, data);
  },
  delete(id) {
    return client.delete(`/admin/banners/${id}`);
  },
  getActive() {
    return client.get('/banners');
  },
};
