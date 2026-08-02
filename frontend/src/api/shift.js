import client from './client';

export { parseShiftEndDatetime, isShiftEndPassed, toLocalDateKey } from './shift-date';
export default {
  getMine() {
    return client.get('/shifts/mine');
  },
  getCurrent() {
    return client.get('/shifts/current');
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
