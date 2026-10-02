import { Link, Route, Routes } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Projects from './pages/Projects';
import CreateProject from './pages/CreateProject';
import ProjectDetail from './pages/ProjectDetail';
import ResultPage from './pages/ResultPage';

export default function App() {
  return (
    <div className="min-h-screen bg-white text-gray-900">
      <nav className="flex gap-4 border-b px-6 py-3">
        <Link to="/" className="font-bold">
          AI Video
        </Link>
        <Link to="/projects" className="text-blue-600">
          Projects
        </Link>
        <Link to="/projects/new" className="text-blue-600">
          + New
        </Link>
      </nav>
      <main className="mx-auto max-w-5xl p-6">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/projects" element={<Projects />} />
          <Route path="/projects/new" element={<CreateProject />} />
          <Route path="/projects/:id" element={<ProjectDetail />} />
          <Route path="/projects/:id/result" element={<ResultPage />} />
          <Route path="*" element={<p>Không tìm thấy trang.</p>} />
        </Routes>
      </main>
    </div>
  );
}
