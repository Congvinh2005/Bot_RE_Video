export interface Project {
  id: string;
  name: string;
  workflowType: 'TIKTOK_PRODUCT' | 'NORMAL_VIDEO';
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface VideoSource {
  id: string;
  projectId: string;
  sourceType: string;
  originalFilename: string | null;
  storageKey: string | null;
  duration: number | null;
  width: number | null;
  height: number | null;
  status: string;
}

export interface TikTokAnalysis {
  status: string;
  message: string;
  metadata: {
    title: string | null;
    author: string | null;
    authorUrl: string | null;
    thumbnailUrl: string | null;
  } | null;
  videoSource: VideoSource | null;
}

export interface JobStatus {
  jobId: string | null;
  status: string;
  progress: number;
  currentStep: string | null;
  errorMessage: string | null;
}

export interface ApiError {
  code: string;
  message: string;
  details?: unknown;
  traceId?: string;
}
