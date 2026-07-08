import client from './client';

export default {
  getAll() {
    return client.get('/favorites');
  },
  check(productId) {
    return client.get(`/favorites/check/${productId}`);
  },
  toggle(productId) {
    return client.post(`/favorites/toggle/${productId}`);
  },
};
