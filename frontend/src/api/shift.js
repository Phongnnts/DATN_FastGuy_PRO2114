import client from './client';

export default {
  getMine() {
    return client.get('/shifts/mine');
  },
  checkIn(id) {
    return client.post(`/shifts/${id}/check-in`);
  },
  checkOut(id) {
    return client.post(`/shifts/${id}/check-out`);
  },
};
