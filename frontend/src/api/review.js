import client from './client';

export default {
  getByOrder(orderId) {
    return client.get(`/reviews/order/${orderId}`);
  },
  create(data) {
    return client.post('/reviews', data);
  },
};
