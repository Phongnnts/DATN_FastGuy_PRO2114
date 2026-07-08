import client from './client';

export default {
  getByProduct(productId) {
    return client.get(`/reviews/product/${productId}`);
  },
  getByOrder(orderId) {
    return client.get(`/reviews/order/${orderId}`);
  },
  create(data) {
    return client.post('/reviews', data);
  },
};
