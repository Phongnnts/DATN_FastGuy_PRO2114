import client from './client';

export default {
  getMine() {
    return client.get('/support');
  },
  create(data) {
    return client.post('/support', data);
  },
  getStaff(all = false) {
    return client.get('/staff/support', { params: all ? { all: true } : {} });
  },
  update(id, data) {
    return client.put(`/staff/support/${id}`, data);
  },
};
