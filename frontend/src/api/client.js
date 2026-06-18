import axios from 'axios';
import { API_BASE_URL } from '@/utils/constants';

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => {
    const body = res.data;
    if (body && body.status === 'error') {
      const msg = body.message || 'Lỗi không xác định';
      return Promise.reject(new Error(msg));
    }
    if (body && body.status === 'success') {
      return body.data !== undefined ? body.data : body;
    }
    return body;
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    const msg = err.response?.data?.message || err.message || 'Lỗi không xác định';
    return Promise.reject(new Error(msg));
  },
);

export default client;
