import client from './client';

export default {
  getProvinces() {
    return client.get('/shipping/provinces');
  },
  getDistricts(provinceId) {
    return client.get('/shipping/districts', { params: { provinceId } });
  },
  getWards(districtId) {
    return client.get('/shipping/wards', { params: { districtId } });
  },
  calculateFee(data) {
    return client.post('/shipping/fee', data);
  },
};
