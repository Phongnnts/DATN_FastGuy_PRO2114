import client from './client';

export default {
  getConfig() {
    return client.get('/store/config');
  },
};
