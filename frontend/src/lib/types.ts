export type TaskStatus = "WAITING" | "PROCESSING" | "SUCCEEDED" | "FAILED";

export type TaskStage =
  | "VALIDATION"
  | "GENERATION"
  | "ASSEMBLY"
  | "COMPLETED";

export interface User {
  id: string;
  displayName: string;
  email: string;
  avatarColor: string;
}

export interface AuthResponse {
  currentUser: User | null;
  users: User[];
}

export interface TemplateSummary {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export type TemplateDetail = TemplateSummary;

export interface TemplateListResponse {
  templates: TemplateSummary[];
  categories: string[];
}

export interface UploadedAsset {
  id: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  width: number;
  height: number;
}

export interface ArtworkTask {
  id: string;
  templateId: string;
  templateTitle: string;
  coverUrl: string;
  assetCount: number;
  status: TaskStatus;
  stage?: TaskStage;
  failedStage?: TaskStage;
  failureReason?: string;
  resultUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SimulationSettings {
  stepDelayMs: number;
  failStage: TaskStage | null;
}

export interface ApiErrorBody {
  code?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
}
