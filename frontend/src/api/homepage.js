import client from './client';

export default {
  get() {
    return client.get('/homepage');
  },
};
