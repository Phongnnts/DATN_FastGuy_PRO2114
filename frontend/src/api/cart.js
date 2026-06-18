import client from './client';

export default {
  get() {
    return client.get('/cart');
  },
  addItem(data) {
    return client.post('/cart', data);
  },
  updateItem(cartItemId, data) {
    return client.put('/cart', { cartItemId, ...data });
  },
  removeItem(cartItemId) {
    return client.delete(`/cart/${cartItemId}`);
  },
};
