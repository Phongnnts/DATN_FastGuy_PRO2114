import axios from 'axios';
import { API_BASE_URL } from '@/utils/constants';
import { clearStoredSession, isTokenValid } from '@/utils/session';
import { normalizeApiError } from './error';

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) config.headers.delete('Content-Type');
  const token = localStorage.getItem('token');
  if (isTokenValid(token)) {
    config.headers.Authorization = `Bearer ${token}`;
  } else if (token) {
    clearStoredSession();
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
    if (err.response?.status === 401 && !err.config?.suppressAuthRedirect) {
      clearStoredSession();
      if (window.location.pathname !== '/') {
        const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`;
        window.location.replace(`/?redirect=${encodeURIComponent(redirect)}`);
      }
    }
    return Promise.reject(normalizeApiError(err));
  },
);

export default client;
