import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiErrorMessage } from '../api';
import type { Project } from '../types';

export default function Projects() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [error, setError] = useState('');

  const load = () =>
    client
      .get<Project[]>('/projects')
      .then((r) => setProjects(r.data))
      .catch((e) => setError(apiErrorMessage(e)));

  useEffect(() => {
    void load();
  }, []);

  const remove = async (id: string) => {
    if (!window.confirm('Xóa project này?')) return;
    try {
      await client.delete(`/projects/${id}`);
      await load();
    } catch (e) {
      setError(apiErrorMessage(e));
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Projects</h1>
        <Link
          to="/projects/new"
          className="rounded bg-blue-600 px-4 py-2 text-white"
        >
          + Tạo project
        </Link>
      </div>
      {error && <p className="text-red-600">{error}</p>}
      <ul className="divide-y rounded border">
        {projects.map((p) => (
          <li key={p.id} className="flex items-center justify-between p-3">
            <div>
              <Link
                to={`/projects/${p.id}`}
                className="font-medium text-blue-700"
              >
                {p.name}
              </Link>
              <p className="text-sm text-gray-500">
                {p.workflowType} • {p.status}
              </p>
            </div>
            <button
              onClick={() => void remove(p.id)}
              className="rounded border px-3 py-1 text-sm text-red-600"
            >
              Xóa
            </button>
          </li>
        ))}
        {projects.length === 0 && (
          <li className="p-3 text-gray-500">Chưa có project nào.</li>
        )}
      </ul>
    </div>
  );
}
