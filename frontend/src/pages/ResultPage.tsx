import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import client, { apiErrorMessage } from '../api';
import type { ProjectResult } from '../resultTypes';

export default function ResultPage() {
  const { id } = useParams<{ id: string }>();
  const [result, setResult] = useState<ProjectResult | null>(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  const [hook, setHook] = useState('');
  const [script, setScript] = useState('');
  const [caption, setCaption] = useState('');
  const [hashtags, setHashtags] = useState('');
  const [cta, setCta] = useState('');

  const load = useCallback(async () => {
    if (!id) return;
    try {
      const res = await client.get<ProjectResult>(`/projects/${id}/result`);
      setResult(res.data);
      const c = res.data.content;
      if (c) {
        setHook(c.hook ?? '');
        setScript(c.script ?? '');
        setCaption(c.caption ?? '');
        setHashtags((c.hashtags ?? []).join(' '));
        setCta(c.cta ?? '');
      }
    } catch (e) {
      setError(apiErrorMessage(e));
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const run = async (label: string, fn: () => Promise<unknown>) => {
    setBusy(true);
    setError('');
    setMsg('');
    try {
      await fn();
      setMsg(`${label} xong`);
      await load();
    } catch (e) {
      setError(apiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const saveEdit = () =>
    run('Lưu bản sửa', () =>
      client
        .put(`/projects/${id}/content`, {
          hook,
          script,
          caption,
          hashtags: hashtags.split(/\s+/).filter(Boolean),
          cta,
        })
        .then((r) => r.data),
    );

  const regenContent = () =>
    run('Tạo content mới', () =>
      client.post(`/projects/${id}/generate-content`, {}).then((r) => r.data),
    );

  const regenVoice = () => {
    const contentId = result?.content?.id;
    if (!contentId) {
      setError('Chưa có content để sinh voice');
      return Promise.resolve();
    }
    return run('Sinh voice ADAM mới', () =>
      client
        .post(`/projects/${id}/generate-voice`, {
          contentGenerationId: contentId,
          voice: 'ADAM',
        })
        .then((r) => r.data),
    );
  };

  const regenVideo = () => {
    const contentId = result?.content?.id;
    const voiceId = result?.voice?.id;
    if (!contentId || !voiceId) {
      setError('Cần cả content và voice trước khi dựng video');
      return Promise.resolve();
    }
    return run('Dựng video mới', () =>
      client
        .post(`/projects/${id}/generate-video`, {
          contentGenerationId: contentId,
          voiceGenerationId: voiceId,
        })
        .then((r) => r.data),
    );
  };

  const inputCls = 'w-full rounded border px-3 py-2';
  const btn = 'rounded border px-3 py-2 text-sm disabled:opacity-50';
  const btnPrimary = 'rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50';

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Kết quả</h1>
        <Link to={`/projects/${id}`} className="text-blue-600">
          ← Về project
        </Link>
      </div>
      {error && <p className="text-red-600">{error}</p>}
      {msg && <p className="text-green-700">{msg}</p>}
      {!result && !error && <p>Đang tải...</p>}

      {result && (
        <div className="grid gap-5 md:grid-cols-2">
          <div className="space-y-3">
            <h2 className="font-semibold">Video Preview</h2>
            {result.video?.videoUrl ? (
              <video
                src={result.video.videoUrl}
                controls
                className="aspect-[9/16] w-full max-w-xs rounded bg-black"
              />
            ) : (
              <p className="text-gray-500">Chưa có video. Hãy dựng video trước.</p>
            )}
            {result.video && (
              <a
                href={result.video.videoUrl ?? '#'}
                download
                className="inline-block rounded bg-green-600 px-4 py-2 text-white"
              >
                Download
              </a>
            )}
            {result.voice?.audioUrl && (
              <div>
                <h3 className="text-sm font-semibold">Voice ADAM</h3>
                <audio src={result.voice.audioUrl} controls className="w-full" />
              </div>
            )}
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold">
                Content {result.content && `(v${result.content.version})`}
              </h2>
              <button disabled={busy} className={btnPrimary} onClick={() => void saveEdit()}>
                Edit Content
              </button>
            </div>
            <label className="block text-sm">
              Hook
              <input className={inputCls} value={hook} onChange={(e) => setHook(e.target.value)} />
            </label>
            <label className="block text-sm">
              Script (sửa trước khi sinh voice)
              <textarea
                className={inputCls}
                rows={6}
                value={script}
                onChange={(e) => setScript(e.target.value)}
              />
            </label>
            <label className="block text-sm">
              Caption
              <textarea
                className={inputCls}
                rows={2}
                value={caption}
                onChange={(e) => setCaption(e.target.value)}
              />
            </label>
            <label className="block text-sm">
              Hashtags (cách nhau bằng dấu cách)
              <input
                className={inputCls}
                value={hashtags}
                onChange={(e) => setHashtags(e.target.value)}
              />
            </label>
            <label className="block text-sm">
              CTA
              <input className={inputCls} value={cta} onChange={(e) => setCta(e.target.value)} />
            </label>

            <div className="flex flex-wrap gap-2 pt-2">
              <button disabled={busy} className={btn} onClick={() => void regenContent()}>
                Regenerate Content
              </button>
              <button disabled={busy} className={btn} onClick={() => void regenVoice()}>
                Regenerate Voice
              </button>
              <button disabled={busy} className={btn} onClick={() => void regenVideo()}>
                Regenerate Video
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
