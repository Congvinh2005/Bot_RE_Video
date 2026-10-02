import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import client, { apiErrorMessage } from '../api';
import { useJobStatus } from '../components/useJobStatus';
import JobProgress from '../components/JobProgress';
import type { Project } from '../types';

export default function ProjectDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);
  const [tracking, setTracking] = useState(false);
  const { job } = useJobStatus(id ?? '', tracking);

  useEffect(() => {
    if (!id) return;
    client
      .get<Project>(`/projects/${id}`)
      .then((r) => setProject(r.data))
      .catch((e) => setError(apiErrorMessage(e)));
  }, [id]);

  const call = async (label: string, fn: () => Promise<unknown>, track = false) => {
    if (!id) return;
    setBusy(true);
    setError('');
    setMsg('');
    try {
      const res = await fn();
      setMsg(`${label} OK: ${JSON.stringify(res).slice(0, 200)}`);
      if (track) setTracking(true);
    } catch (e) {
      setError(apiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const post = (path: string, body?: unknown) =>
    client.post(`/projects/${id}${path}`, body).then((r) => r.data);

  if (!project) return <p>{error || 'Đang tải...'}</p>;

  const btn = 'rounded border px-3 py-2 text-sm disabled:opacity-50';

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{project.name}</h1>
        <Link to={`/projects/${project.id}/result`} className="text-blue-600">
          → Trang kết quả
        </Link>
      </div>
      <p className="text-sm text-gray-500">
        {project.workflowType} • {project.status}
      </p>

      <div className="rounded border p-4">
        <h2 className="mb-2 font-semibold">Chạy full pipeline</h2>
        <button
          disabled={busy}
          className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
          onClick={() => void call('Generate full', () => post('/generate-full', {}), true)}
        >
          Generate Full Video
        </button>
      </div>

      <div className="rounded border p-4">
        <h2 className="mb-2 font-semibold">Chạy từng bước</h2>
        <div className="flex flex-wrap gap-2">
          <button disabled={busy} className={btn} onClick={() => void call('Analyze', () => post('/analyze', {}), true)}>
            1. Analyze
          </button>
          <button disabled={busy} className={btn} onClick={() => void call('Content', () => post('/generate-content', {}))}>
            2. Content
          </button>
          <button disabled={busy} className={btn} onClick={() => void call('Status check', () => client.get(`/projects/${id}/status`).then((r) => r.data))}>
            3. Status
          </button>
        </div>
        <p className="mt-2 text-sm text-gray-500">
          Voice/subtitle/video cần ID cụ thể — thao tác ở trang kết quả (Task 17).
        </p>
      </div>

      <div className="rounded border p-4">
        <h2 className="mb-2 font-semibold">Tiến độ job</h2>
        <button
          className={btn}
          onClick={() => setTracking((t) => !t)}
        >
          {tracking ? 'Dừng theo dõi' : 'Theo dõi job'}
        </button>
        <div className="mt-2">
          <JobProgress job={job} />
        </div>
      </div>

      {msg && <p className="break-all text-sm text-green-700">{msg}</p>}
      {error && <p className="text-red-600">{error}</p>}

      <button
        className="text-sm text-red-600"
        onClick={() =>
          id &&
          window.confirm('Xóa project?') &&
          client
            .delete(`/projects/${id}`)
            .then(() => navigate('/projects'))
            .catch((e) => setError(apiErrorMessage(e)))
        }
      >
        Xóa project
      </button>
    </div>
  );
}
