import client from './client';

export default {
  getMe() {
    return client.get('/loyalty/me');
  },
};
