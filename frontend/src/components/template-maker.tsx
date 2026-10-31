"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  ArrowLeft,
  ArrowUpRight,
  Check,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Trash2,
  UploadCloud,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { TaskHistoryPanel } from "@/components/task-history-panel";
import { apiRequest, deleteAsset, uploadAsset } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import type {
  ArtworkTask,
  TemplateDetail,
  UploadedAsset,
} from "@/lib/types";

interface UploadSlotState {
  asset: UploadedAsset | null;
  previewUrl: string | null;
  fileName: string | null;
  uploading: boolean;
  error: string | null;
}

export function TemplateMaker({ templateId }: { templateId: string }) {
  const { currentUser } = useAuth();
  const templateQuery = useQuery({
    queryKey: currentUser
      ? queryKeys.template(currentUser.id, templateId)
      : ["template", "anonymous", templateId],
    queryFn: ({ signal }) =>
      apiRequest<TemplateDetail>(`/api/templates/${templateId}`, { signal }),
    enabled: Boolean(currentUser),
  });

  if (templateQuery.isPending) {
    return <MakerSkeleton />;
  }
  if (templateQuery.isError) {
    return (
      <div className="page-wrap">
        <div className="large-state maker-error-state">
          <AlertCircle size={28} />
          <h1>无法打开这个模板</h1>
          <p>{templateQuery.error.message}</p>
          <div className="button-row">
            <Link className="secondary-button" href="/">
              返回模板库
            </Link>
            <button
              className="primary-button"
              type="button"
              onClick={() => void templateQuery.refetch()}
            >
              <RefreshCw size={16} />
              重试
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <MakerWorkspace
      key={templateQuery.data.id}
      template={templateQuery.data}
      userId={currentUser?.id ?? ""}
    />
  );
}

function MakerWorkspace({
  template,
  userId,
}: {
  template: TemplateDetail;
  userId: string;
}) {
  const queryClient = useQueryClient();
  const [slots, setSlots] = useState<UploadSlotState[]>(() =>
    emptySlots(template.requiredImageCount),
  );
  const [successMessage, setSuccessMessage] = useState("");
  const controllersRef = useRef(new Map<number, AbortController>());
  const objectUrlsRef = useRef(new Set<string>());
  const submittingRef = useRef(false);

  useEffect(
    () => () => {
      controllersRef.current.forEach((controller) => controller.abort());
      objectUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
    },
    [],
  );

  const readyCount = slots.filter((slot) => slot.asset).length;
  const isUploading = slots.some((slot) => slot.uploading);
  const isReady = readyCount === template.requiredImageCount && !isUploading;
  const previewUrl = slots.find((slot) => slot.previewUrl)?.previewUrl;

  const createMutation = useMutation({
    mutationFn: () =>
      apiRequest<ArtworkTask>("/api/tasks", {
        method: "POST",
        body: JSON.stringify({
          templateId: template.id,
          assetIds: slots.map((slot) => slot.asset?.id),
        }),
      }),
    onSuccess: (task) => {
      queryClient.setQueryData<ArtworkTask[]>(
        queryKeys.tasks(userId),
        (current) => (current ? [task, ...current] : [task]),
      );
      setSuccessMessage("任务已受理，后台正在继续处理。");
      revokeAllObjectUrls(objectUrlsRef.current);
      objectUrlsRef.current.clear();
      setSlots(emptySlots(template.requiredImageCount));
    },
    onSettled: () => {
      submittingRef.current = false;
    },
  });

  const updateSlot = (
    index: number,
    update: Partial<UploadSlotState>,
  ) => {
    setSlots((current) =>
      current.map((slot, slotIndex) =>
        slotIndex === index ? { ...slot, ...update } : slot,
      ),
    );
  };

  const chooseFile = async (index: number, file: File) => {
    setSuccessMessage("");
    if (
      file.type &&
      !template.acceptedMimeTypes.includes(file.type)
    ) {
      updateSlot(index, {
        error: "请选择 JPEG 或 PNG 图片",
        uploading: false,
      });
      return;
    }
    if (file.type === "" && !/\.(jpe?g|png)$/i.test(file.name)) {
      updateSlot(index, {
        error: "无法识别图片格式，请选择 JPEG 或 PNG",
        uploading: false,
      });
      return;
    }
    if (file.size > template.maxFileSizeBytes) {
      updateSlot(index, {
        error: `图片不能超过 ${formatBytes(template.maxFileSizeBytes)}`,
        uploading: false,
      });
      return;
    }

    controllersRef.current.get(index)?.abort();
    const controller = new AbortController();
    controllersRef.current.set(index, controller);
    const previewUrl = URL.createObjectURL(file);
    objectUrlsRef.current.add(previewUrl);

    setSlots((current) =>
      current.map((slot, slotIndex) => {
        if (slotIndex !== index) {
          return slot;
        }
        if (slot.previewUrl) {
          URL.revokeObjectURL(slot.previewUrl);
          objectUrlsRef.current.delete(slot.previewUrl);
        }
        return {
          ...slot,
          previewUrl,
          fileName: file.name,
          uploading: true,
          error: null,
          asset: null,
        };
      }),
    );
    const previousAssetId = slots[index]?.asset?.id;

    try {
      const asset = await uploadAsset(file, controller.signal);
      if (controller.signal.aborted) {
        void deleteAsset(asset.id).catch(() => undefined);
        return;
      }
      updateSlot(index, { asset, uploading: false, error: null });
      if (previousAssetId) {
        void deleteAsset(previousAssetId).catch(() => undefined);
      }
    } catch (error) {
      if (controller.signal.aborted) {
        return;
      }
      updateSlot(index, {
        uploading: false,
        error: error instanceof Error ? error.message : "上传失败，请重试",
      });
    } finally {
      controllersRef.current.delete(index);
    }
  };

  const removeAsset = (index: number) => {
    controllersRef.current.get(index)?.abort();
    controllersRef.current.delete(index);
    const slot = slots[index];
    if (slot.previewUrl) {
      URL.revokeObjectURL(slot.previewUrl);
      objectUrlsRef.current.delete(slot.previewUrl);
    }
    if (slot.asset) {
      void deleteAsset(slot.asset.id).catch(() => undefined);
    }
    setSlots((current) =>
      current.map((item, slotIndex) =>
        slotIndex === index
          ? {
              asset: null,
              previewUrl: null,
              fileName: null,
              uploading: false,
              error: null,
            }
          : item,
      ),
    );
  };

  return (
    <div className="page-wrap maker-page">
      <Link href="/" className="back-link">
        <ArrowLeft size={16} />
        返回模板库
      </Link>

      <header className="maker-heading">
        <div>
          <p className="eyebrow">{template.category}</p>
          <h1>{template.title}</h1>
          <p>{template.description}</p>
        </div>
        <span className="maker-index">WORKSHOP / {template.id.toUpperCase()}</span>
      </header>

      <div className="maker-layout">
        <section className="maker-composer" aria-labelledby="composer-title">
          <div className="preview-card">
            <div className="preview-card-top">
              <span>
                <Sparkles size={14} /> 实时预览
              </span>
              <span>{previewUrl ? "已载入素材" : "等待素材"}</span>
            </div>
            <div className="preview-canvas">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={previewUrl || template.coverUrl}
                alt={previewUrl ? "当前上传素材预览" : `${template.title}示例封面`}
              />
              <div className="preview-vignette" />
              <div className="preview-title-card">
                <small>MATERIAL STUDIO</small>
                <strong>{template.title}</strong>
              </div>
            </div>
          </div>

          <div className="upload-section">
            <div className="upload-heading">
              <div>
                <p className="eyebrow">YOUR MATERIALS</p>
                <h2 id="composer-title">准备素材</h2>
              </div>
              <span className={isReady ? "ready-count ready" : "ready-count"}>
                {readyCount}/{template.requiredImageCount} 已就绪
              </span>
            </div>
            <p className="upload-requirements">
              需要 {template.requiredImageCount} 张图片 · 支持 JPEG / PNG · 单张不超过{" "}
              {formatBytes(template.maxFileSizeBytes)}
            </p>
            <div className="upload-slots">
              {slots.map((slot, index) => (
                <UploadSlot
                  key={`${template.id}-${index}`}
                  index={index}
                  slot={slot}
                  acceptedMimeTypes={template.acceptedMimeTypes}
                  onChoose={(file) => void chooseFile(index, file)}
                  onRemove={() => removeAsset(index)}
                />
              ))}
            </div>

            {createMutation.isError ? (
              <p className="form-error" role="alert">
                <AlertCircle size={16} />
                {createMutation.error.message}
              </p>
            ) : null}
            {successMessage ? (
              <p className="form-success" role="status">
                <Check size={16} />
                {successMessage}
              </p>
            ) : null}

            <button
              className="create-button"
              type="button"
              disabled={!isReady || createMutation.isPending}
              onClick={() => {
                if (submittingRef.current || !isReady) {
                  return;
                }
                submittingRef.current = true;
                createMutation.mutate();
              }}
            >
              {createMutation.isPending ? (
                <LoaderCircle size={18} className="spin" />
              ) : (
                <Sparkles size={18} />
              )}
              {createMutation.isPending
                ? "正在提交任务"
                : isUploading
                  ? "素材上传中"
                  : "开始制作"}
              <span>→</span>
            </button>
          </div>
        </section>

        <aside className="maker-history">
          <div className="history-sidebar-note">
            <ShieldCheck size={18} />
            <p>
              提交后可以关闭页面。任务由 Java 后台继续执行，稍后回到这里查看。
            </p>
          </div>
          <TaskHistoryPanel userId={userId} templateId={template.id} compact />
          <Link href="/history" className="sidebar-history-link">
            查看全部个人制作
            <ArrowUpRight size={16} />
          </Link>
        </aside>
      </div>
    </div>
  );
}

function UploadSlot({
  index,
  slot,
  acceptedMimeTypes,
  onChoose,
  onRemove,
}: {
  index: number;
  slot: UploadSlotState;
  acceptedMimeTypes: string[];
  onChoose: (file: File) => void;
  onRemove: () => void;
}) {
  const inputId = `upload-${index}`;
  return (
    <div className={`upload-slot ${slot.asset ? "has-asset" : ""}`}>
      <input
        id={inputId}
        className="visually-hidden"
        type="file"
        accept={acceptedMimeTypes.join(",")}
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) {
            onChoose(file);
          }
          event.target.value = "";
        }}
      />
      {slot.previewUrl ? (
        <div className="upload-preview">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={slot.previewUrl} alt={`第 ${index + 1} 张素材预览`} />
          {slot.uploading ? (
            <span className="upload-overlay">
              <LoaderCircle className="spin" size={19} />
              上传中
            </span>
          ) : null}
          <div className="upload-controls">
            <label className="small-action" htmlFor={inputId}>
              替换
            </label>
            <button
              className="icon-button small-icon-button"
              type="button"
              aria-label={`移除第 ${index + 1} 张素材`}
              onClick={onRemove}
            >
              <Trash2 size={15} />
            </button>
          </div>
        </div>
      ) : (
        <label className="upload-empty" htmlFor={inputId}>
          <span className="upload-empty-icon">
            {slot.error ? <AlertCircle size={21} /> : <UploadCloud size={21} />}
          </span>
          <strong>{slot.error ? "重新选择" : `上传素材 ${index + 1}`}</strong>
          <span>JPEG 或 PNG</span>
        </label>
      )}
      {slot.error ? (
        <p className="slot-error" role="alert">
          {slot.error}
        </p>
      ) : null}
    </div>
  );
}

function emptySlots(count: number): UploadSlotState[] {
  return Array.from({ length: count }, () => ({
    asset: null,
    previewUrl: null,
    fileName: null,
    uploading: false,
    error: null,
  }));
}

function revokeAllObjectUrls(urls: Set<string>) {
  urls.forEach((url) => URL.revokeObjectURL(url));
}

function formatBytes(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${Math.round(bytes / 1024 / 1024)}MB`;
  }
  return `${Math.round(bytes / 1024)}KB`;
}

function MakerSkeleton() {
  return (
    <div className="page-wrap maker-page">
      <span className="skeleton-line narrow" />
      <div className="maker-layout">
        <div className="maker-composer">
          <div className="skeleton-block maker-skeleton-preview" />
          <span className="skeleton-line wide" />
          <span className="skeleton-line medium" />
        </div>
        <div className="maker-history">
          <div className="skeleton-block maker-skeleton-history" />
        </div>
      </div>
    </div>
  );
}
