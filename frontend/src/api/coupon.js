import client from './client';

export default {
  getAll() {
    return client.get('/admin/coupons');
  },
  getById(id) {
    return client.get(`/admin/coupons/${id}`);
  },
  create(data) {
    return client.post('/admin/coupons', data);
  },
  update(id, data) {
    return client.put(`/admin/coupons/${id}`, data);
  },
  delete(id) {
    return client.delete(`/admin/coupons/${id}`);
  },
  verify(code, totalAmount, shippingFee) {
    return client.post('/coupons/verify', { code, totalAmount, shippingFee });
  },
  getPublic() {
    return client.get('/coupons/public');
  },
  getClaimed() {
    return client.get('/coupons/claimed');
  },
  claim(couponId) {
    return client.post('/coupons/claim', { couponId });
  },
};
