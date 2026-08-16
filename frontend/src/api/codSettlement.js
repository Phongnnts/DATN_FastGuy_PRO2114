import client from './client';

export default {
  getCurrent() { return client.get('/cod-settlements/current'); },
  getMine() { return client.get('/cod-settlements/mine'); },
  submit(data) { return client.post('/cod-settlements', data); },
  getAdmin(status) { return client.get('/cod-settlements/admin', { params: { status } }); },
  verify(id, data) { return client.put(`/cod-settlements/${id}/verify`, data); },
};
