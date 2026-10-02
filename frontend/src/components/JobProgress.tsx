import type { JobStatus } from '../types';

const STEPS = ['ANALYZE', 'CONTENT', 'VOICE', 'SUBTITLE', 'VIDEO', 'EXPORT'];

export default function JobProgress({ job }: { job: JobStatus | null }) {
  if (!job) return <p className="text-gray-500">Chưa có job nào chạy.</p>;
  const stepIndex = job.currentStep ? STEPS.indexOf(job.currentStep) : -1;
  return (
    <div className="rounded border p-4">
      <div className="flex justify-between text-sm">
        <span className="font-semibold">{job.status}</span>
        <span>{job.progress}%</span>
      </div>
      <div className="mt-2 h-2 rounded bg-gray-200">
        <div
          className="h-2 rounded bg-blue-600 transition-all"
          style={{ width: `${job.progress}%` }}
        />
      </div>
      <ol className="mt-3 space-y-1 text-sm">
        {STEPS.map((s, i) => (
          <li key={s} className={i <= stepIndex ? 'text-green-700' : 'text-gray-400'}>
            {i <= stepIndex ? '✓' : '○'} {s}
          </li>
        ))}
      </ol>
      {job.errorMessage && (
        <p className="mt-2 text-sm text-red-600">Lỗi: {job.errorMessage}</p>
      )}
    </div>
  );
}
