import type { ApiErrorBody } from "@/lib/types";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors?: Record<string, string>;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || "请求失败，请稍后重试");
    this.name = "ApiError";
    this.status = status;
    this.code = body.code || "REQUEST_FAILED";
    this.fieldErrors = body.fieldErrors;
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {
    ...options,
    credentials: "same-origin",
    headers,
  });

  if (!response.ok) {
    let body: ApiErrorBody = {};
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = { message: `请求失败（${response.status}）` };
    }
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function uploadAsset(
  file: File,
  signal?: AbortSignal,
): Promise<import("@/lib/types").UploadedAsset> {
  const body = new FormData();
  body.append("file", file);
  return apiRequest("/api/assets", {
    method: "POST",
    body,
    signal,
  });
}

export function deleteAsset(assetId: string): Promise<void> {
  return apiRequest(`/api/assets/${assetId}`, { method: "DELETE" });
}
