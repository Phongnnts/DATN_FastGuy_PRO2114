import client from './client';

export default {
  getByProduct(productId, params = { page: 1, size: 10 }) {
    return client.get(`/reviews/product/${productId}`, { params });
  },
  getByOrder(orderId) {
    return client.get(`/reviews/order/${orderId}`);
  },
  create(data) {
    return client.post('/reviews', data);
  },
};
