import axios from 'axios';

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const api = axios.create({ baseURL: BASE_URL });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('jwt_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const authApi = {
  login: (email, password) => api.post('/api/auth/login', { email, password }),
  register: (data) => api.post('/api/auth/register', data),
};

export const pairApi = {
  getMe:           ()         => api.get('/api/users/me'),
  getChildDevices: ()         => api.get('/api/child-devices'),
  unpair:          (deviceId) => api.post(`/api/unpair-device/${deviceId}`),
};

export const deviceApi = {
  // Poll child device status directly from REST API
  getStatus: () => api.get('/api/child-devices'),
};

export const locationApi = {
  getLatest: (deviceId) => api.get(`/api/location/${deviceId}/latest`),
  getHistory: (deviceId, limit = 100) => api.get(`/api/location/${deviceId}/history`, { params: { limit } }),
};

export const commandApi = {
  send: (targetDeviceId, commandType, metadata = null) =>
    api.post('/api/send-command', { targetDeviceId, commandType, metadata }),
  sendApp: (targetDeviceId, commandType, packageName) =>
    api.post('/api/send-command', { targetDeviceId, commandType, packageName }),
  getHistory: (deviceId) => api.get(`/api/commands/${deviceId}`),
};

export const appApi = {
  getControls:  (deviceId) => api.get(`/api/apps/controls/${deviceId}`),
  getInstalled: (deviceId) => api.get(`/api/apps/installed/${deviceId}`),
  setControl:   (data)     => api.post('/api/apps/control', data),
  removeControl:(deviceId, packageName, controlType) =>
    api.delete(`/api/apps/control/${deviceId}/${packageName}`, { params: { controlType } }),
};

export default api;
