import client from './client';

export default {
  getAll() {
    return client.get('/delivery-zones');
  }
};
