import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiErrorMessage } from '../api';
import type { Project } from '../types';

export default function Dashboard() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    client
      .get<Project[]>('/projects')
      .then((r) => setProjects(r.data))
      .catch((e) => setError(apiErrorMessage(e)));
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">AI Video Generator</h1>
      {error && <p className="text-red-600">{error}</p>}
      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded border p-4">
          <p className="text-sm text-gray-500">Tổng project</p>
          <p className="text-3xl font-bold">{projects.length}</p>
        </div>
        <div className="rounded border p-4">
          <p className="text-sm text-gray-500">TikTok Product</p>
          <p className="text-3xl font-bold">
            {projects.filter((p) => p.workflowType === 'TIKTOK_PRODUCT').length}
          </p>
        </div>
        <div className="rounded border p-4">
          <p className="text-sm text-gray-500">Normal Video</p>
          <p className="text-3xl font-bold">
            {projects.filter((p) => p.workflowType === 'NORMAL_VIDEO').length}
          </p>
        </div>
      </div>
      <div>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-lg font-semibold">Project gần đây</h2>
          <Link to="/projects" className="text-blue-600">
            Xem tất cả
          </Link>
        </div>
        <ul className="divide-y rounded border">
          {projects.slice(0, 5).map((p) => (
            <li key={p.id} className="flex justify-between p-3">
              <Link to={`/projects/${p.id}`} className="font-medium text-blue-700">
                {p.name}
              </Link>
              <span className="text-sm text-gray-500">{p.status}</span>
            </li>
          ))}
          {projects.length === 0 && (
            <li className="p-3 text-gray-500">Chưa có project nào.</li>
          )}
        </ul>
      </div>
    </div>
  );
}
