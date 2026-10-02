import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
  timeout: 120000,
});

export function apiErrorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as { code?: string; message?: string } | undefined;
    if (data?.message) return `${data.code ?? 'ERROR'}: ${data.message}`;
    return e.message;
  }
  return String(e);
}

export default client;
