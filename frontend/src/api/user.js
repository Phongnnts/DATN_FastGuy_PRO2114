import client from './client';

export default {
  getAddresses() {
    return client.get('/user/addresses');
  },
  createAddress(data) {
    return client.post('/user/addresses', data);
  },
  updateAddress(id, data) {
    return client.put(`/user/addresses/${id}`, data);
  },
  deleteAddress(id) {
    return client.delete(`/user/addresses/${id}`);
  },
  setDefaultAddress(id) {
    return client.put(`/user/addresses/${id}/default`);
  },
  getFavorites() {
    return client.get('/user/favorites');
  },
  addFavorite(productId) {
    return client.post('/user/favorites', { productId });
  },
  removeFavorite(productId) {
    return client.delete(`/user/favorites/${productId}`);
  },
  getVouchers() {
    return client.get('/user/vouchers');
  },
};
