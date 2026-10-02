export interface ContentItem {
  id: string;
  projectId: string;
  version: number;
  hook: string | null;
  script: string | null;
  caption: string | null;
  hashtags: string[] | null;
  cta: string | null;
  scenes: Array<{
    start: number;
    end: number;
    voiceText: string;
    overlayText: string;
    purpose?: string;
  }> | null;
  createdAt: string;
}

export interface VoiceItem {
  id: string;
  projectId: string;
  contentGenerationId: string | null;
  voice: string;
  status: string;
  storageKey: string | null;
  audioUrl: string | null;
}

export interface VideoItem {
  id: string;
  projectId: string;
  storageKey: string;
  videoUrl: string | null;
  thumbnailUrl: string | null;
  duration: number | null;
  width: number;
  height: number;
  status: string;
}

export interface ProjectResult {
  content: ContentItem | null;
  voice: VoiceItem | null;
  video: VideoItem | null;
  product: {
    name: string | null;
    productUrl: string | null;
  } | null;
}
