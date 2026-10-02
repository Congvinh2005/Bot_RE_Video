import type { ReactNode } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Projects from './pages/Projects';
import CreateProject from './pages/CreateProject';
import ProjectDetail from './pages/ProjectDetail';
import ResultPage from './pages/ResultPage';
import Login from './pages/Login';
import { clearSession, getEmail, getToken } from './api';

function RequireAuth({ children }: { children: ReactNode }) {
  if (!getToken()) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function Nav() {
  const navigate = useNavigate();
  const email = getEmail();
  return (
    <nav className="flex items-center gap-4 border-b px-6 py-3">
      <Link to="/" className="font-bold">
        AI Video
      </Link>
      <Link to="/projects" className="text-blue-600">
        Projects
      </Link>
      <Link to="/projects/new" className="text-blue-600">
        + New
      </Link>
      <span className="flex-1" />
      {email ? (
        <>
          <span className="text-sm text-gray-500">{email}</span>
          <button
            className="text-sm text-red-600"
            onClick={() => {
              clearSession();
              navigate('/login');
            }}
          >
            Đăng xuất
          </button>
        </>
      ) : (
        <Link to="/login" className="text-sm text-blue-600">
          Đăng nhập
        </Link>
      )}
    </nav>
  );
}

export default function App() {
  return (
    <div className="min-h-screen bg-white text-gray-900">
      <Nav />
      <main className="mx-auto max-w-5xl p-6">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <Dashboard />
              </RequireAuth>
            }
          />
          <Route
            path="/projects"
            element={
              <RequireAuth>
                <Projects />
              </RequireAuth>
            }
          />
          <Route
            path="/projects/new"
            element={
              <RequireAuth>
                <CreateProject />
              </RequireAuth>
            }
          />
          <Route
            path="/projects/:id"
            element={
              <RequireAuth>
                <ProjectDetail />
              </RequireAuth>
            }
          />
          <Route
            path="/projects/:id/result"
            element={
              <RequireAuth>
                <ResultPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<p>Không tìm thấy trang.</p>} />
        </Routes>
      </main>
    </div>
  );
}
