import client from './client';

export default {
  getDeliveries(params) {
    return client.get('/shipper/deliveries', { params });
  },
  getDeliveryById(id) {
    return client.get(`/shipper/deliveries/${id}`);
  },
  acceptDelivery(id) {
    return client.put(`/shipper/deliveries/${id}/accept`);
  },
  updateDeliveryStatus(id, status, data) {
    return client.put(`/shipper/deliveries/${id}/status`, { status, ...data });
  },
  saveDeliveryNote(id, note) {
    return client.post(`/shipper/deliveries/${id}/notes`, { note });
  },
  removeDelivery(id) {
    return client.delete(`/shipper/deliveries/${id}`);
  },
  getHistory(params) {
    return client.get('/shipper/history', { params });
  },
  checkIn() {
    return client.post('/shipper/shifts/check-in');
  },
  checkOut() {
    return client.post('/shipper/shifts/check-out');
  },
  getShiftStatus() {
    return client.get('/shipper/shifts/current');
  },
};
