import axios from 'axios';

const TOKEN_KEY = 'aivideo_token';
const EMAIL_KEY = 'aivideo_email';

const client = axios.create({
  baseURL: '/api',
  timeout: 120000,
});

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getEmail(): string | null {
  return localStorage.getItem(EMAIL_KEY);
}

export function saveSession(token: string, email: string) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(EMAIL_KEY, email);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EMAIL_KEY);
}

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      clearSession();
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(err);
  },
);

export function apiErrorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as
      | { code?: string; message?: string; details?: unknown }
      | undefined;
    if (data?.message) {
      let msg = `${data.code ?? 'ERROR'}: ${data.message}`;
      if (data.details && typeof data.details === 'object') {
        const fields = Object.entries(data.details as Record<string, string>)
          .map(([k, v]) => `${k}: ${v}`)
          .join('; ');
        if (fields) msg += ` (${fields})`;
      }
      return msg;
    }
    return e.message;
  }
  return String(e);
}

export default client;
