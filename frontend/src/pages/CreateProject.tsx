import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client, { apiErrorMessage } from '../api';
import type { Project, TikTokAnalysis } from '../types';

type Mode = 'TIKTOK' | 'NORMAL';

export default function CreateProject() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>('TIKTOK');
  const [name, setName] = useState('');
  const [tiktokUrl, setTiktokUrl] = useState('');
  const [productUrl, setProductUrl] = useState('');
  const [productName, setProductName] = useState('');
  const [context, setContext] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [uploadPct, setUploadPct] = useState(0);
  const [log, setLog] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const pushLog = (m: string) => setLog((l) => [...l, m]);

  const createProject = async (workflowType: Project['workflowType']) => {
    const res = await client.post<Project>('/projects', { name, workflowType });
    pushLog(`✓ Tạo project: ${res.data.id}`);
    return res.data;
  };

  const submitTikTok = async () => {
    setBusy(true);
    setError('');
    setLog([]);
    try {
      const project = await createProject('TIKTOK_PRODUCT');
      const tk = await client.post<TikTokAnalysis>(`/projects/${project.id}/tiktok`, {
        url: tiktokUrl,
        context,
      });
      pushLog(`✓ TikTok: ${tk.data.status} — ${tk.data.message}`);
      if (tk.data.metadata?.author) pushLog(`  Tác giả: ${tk.data.metadata.author}`);
      await client.post(`/projects/${project.id}/product`, {
        productUrl: productUrl || undefined,
        product: productName ? { name: productName, description: context } : undefined,
      });
      pushLog('✓ Lưu product context');
      navigate(`/projects/${project.id}`);
    } catch (e) {
      setError(apiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const submitNormal = async () => {
    if (!file) {
      setError('Hãy chọn file video (mp4/mov/webm)');
      return;
    }
    setBusy(true);
    setError('');
    setLog([]);
    setUploadPct(0);
    try {
      const project = await createProject('NORMAL_VIDEO');
      const form = new FormData();
      form.append('file', file);
      await client.post(`/projects/${project.id}/upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (ev) => {
          if (ev.total) setUploadPct(Math.round((ev.loaded / ev.total) * 100));
        },
      });
      pushLog('✓ Upload + FFprobe xong');
      navigate(`/projects/${project.id}`);
    } catch (e) {
      setError(apiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const inputCls = 'w-full rounded border px-3 py-2';

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h1 className="text-2xl font-bold">Tạo project</h1>
      <div className="grid grid-cols-2 gap-3">
        <button
          onClick={() => setMode('TIKTOK')}
          className={`rounded border p-4 font-semibold ${
            mode === 'TIKTOK' ? 'border-blue-600 bg-blue-50' : ''
          }`}
        >
          [TikTok Product Video]
        </button>
        <button
          onClick={() => setMode('NORMAL')}
          className={`rounded border p-4 font-semibold ${
            mode === 'NORMAL' ? 'border-blue-600 bg-blue-50' : ''
          }`}
        >
          [Normal Video]
        </button>
      </div>

      <input
        className={inputCls}
        placeholder="Tên project"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />

      {mode === 'TIKTOK' ? (
        <>
          <input
            className={inputCls}
            placeholder="TikTok URL (https://www.tiktok.com/@.../video/...)"
            value={tiktokUrl}
            onChange={(e) => setTiktokUrl(e.target.value)}
          />
          <input
            className={inputCls}
            placeholder="Product URL (optional)"
            value={productUrl}
            onChange={(e) => setProductUrl(e.target.value)}
          />
          <input
            className={inputCls}
            placeholder="Tên sản phẩm (nhập tay nếu không có URL)"
            value={productName}
            onChange={(e) => setProductName(e.target.value)}
          />
          <textarea
            className={inputCls}
            rows={3}
            placeholder="Context thêm (optional)"
            value={context}
            onChange={(e) => setContext(e.target.value)}
          />
          <button
            disabled={busy}
            onClick={() => void submitTikTok()}
            className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
          >
            {busy ? 'Đang phân tích...' : 'Analyze'}
          </button>
        </>
      ) : (
        <>
          <input
            type="file"
            accept=".mp4,.mov,.webm"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            className={inputCls}
          />
          {uploadPct > 0 && <p>Uploading: {uploadPct}%</p>}
          <textarea
            className={inputCls}
            rows={3}
            placeholder="Context: bán gì, chất liệu, đối tượng..."
            value={context}
            onChange={(e) => setContext(e.target.value)}
          />
          <p>
            Voice: <strong>ADAM</strong>
          </p>
          <button
            disabled={busy}
            onClick={() => void submitNormal()}
            className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
          >
            {busy ? 'Đang upload...' : 'Generate'}
          </button>
        </>
      )}

      {error && <p className="text-red-600">{error}</p>}
      {log.length > 0 && (
        <ul className="rounded bg-gray-50 p-3 text-sm">
          {log.map((l, i) => (
            <li key={i}>{l}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
