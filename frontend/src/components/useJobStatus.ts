import { useEffect, useRef, useState } from 'react';
import client, { getToken } from '../api';
import type { JobStatus } from '../types';

const TERMINAL = ['COMPLETED', 'FAILED', 'CANCELLED'];

/** SSE trước, tự fallback sang polling 2s khi SSE lỗi. */
export function useJobStatus(projectId: string, active: boolean) {
  const [job, setJob] = useState<JobStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const activeRef = useRef(active);
  activeRef.current = active;

  useEffect(() => {
    if (!active) return;
    let stopped = false;
    let timer: ReturnType<typeof setInterval> | null = null;

    const poll = async () => {
      try {
        const res = await client.get<JobStatus>(`/projects/${projectId}/status`);
        if (!stopped) {
          setJob(res.data);
          if (TERMINAL.includes(res.data.status) && timer) clearInterval(timer);
        }
      } catch {
        if (!stopped) setError('Không lấy được trạng thái job');
      }
    };

    let es: EventSource | null = null;
    try {
      // EventSource không gửi được header Authorization -> kèm token qua query.
      const token = getToken();
      const url =
        `/api/projects/${projectId}/status/stream` +
        (token ? `?token=${encodeURIComponent(token)}` : '');
      es = new EventSource(url);
      es.onmessage = (ev) => {
        if (stopped) return;
        try {
          const data = JSON.parse(ev.data) as JobStatus;
          setJob(data);
          if (TERMINAL.includes(data.status)) es?.close();
        } catch {
          /* ignore malformed event */
        }
      };
      es.onerror = () => {
        es?.close();
        if (!stopped && activeRef.current) {
          void poll();
          timer = setInterval(poll, 2000);
        }
      };
    } catch {
      void poll();
      timer = setInterval(poll, 2000);
    }

    return () => {
      stopped = true;
      es?.close();
      if (timer) clearInterval(timer);
    };
  }, [projectId, active]);

  return { job, error };
}
