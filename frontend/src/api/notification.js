import client from './client';

export default {
  get() {
    return client.get('/notifications');
  },
  markRead(id) {
    return client.put(`/notifications/${id}/read`);
  },
  markAllRead() {
    return client.put('/notifications/read-all');
  },
};
