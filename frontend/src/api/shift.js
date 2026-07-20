import client from './client';

export default {
  getMine() {
    return client.get('/shifts/mine');
  },
  hasToday() {
    return client.get('/shifts/has-today');
  },
  checkIn(id) {
    return client.post(`/shifts/${id}/check-in`);
  },
  checkOut(id) {
    return client.post(`/shifts/${id}/check-out`);
  },
};
