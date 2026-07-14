import client from './client';

export default {
  login(credentials) {
    return client.post('/auth/login', credentials);
  },
  register(data) {
    return client.post('/auth/register', data);
  },
  logout() {
    return client.post('/auth/logout');
  },
  getProfile() {
    return client.get('/auth/profile');
  },
  updateProfile(data) {
    return client.put('/auth/profile', data);
  },
};
