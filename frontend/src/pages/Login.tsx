import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client, { apiErrorMessage, saveSession } from '../api';

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (path: '/auth/register' | '/auth/login') => {
    setBusy(true);
    setError('');
    try {
      const res = await client.post<{ token: string; email: string }>(path, {
        email,
        password,
      });
      saveSession(res.data.token, res.data.email);
      navigate('/');
    } catch (e) {
      setError(apiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const inputCls = 'w-full rounded border px-3 py-2';

  return (
    <div className="mx-auto max-w-sm space-y-4">
      <h1 className="text-2xl font-bold">Đăng nhập</h1>
      <input
        className={inputCls}
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <input
        className={inputCls}
        type="password"
        placeholder="Mật khẩu (tối thiểu 6 ký tự)"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      {error && <p className="text-red-600">{error}</p>}
      <div className="flex gap-2">
        <button
          disabled={busy}
          onClick={() => void submit('/auth/login')}
          className="flex-1 rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
        >
          Đăng nhập
        </button>
        <button
          disabled={busy}
          onClick={() => void submit('/auth/register')}
          className="flex-1 rounded border px-4 py-2 disabled:opacity-50"
        >
          Đăng ký
        </button>
      </div>
    </div>
  );
}
