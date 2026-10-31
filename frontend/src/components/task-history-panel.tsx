"use client";

import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  ArrowUpRight,
  CheckCircle2,
  Clock3,
  ImageIcon,
  LoaderCircle,
  RefreshCw,
  Sparkles,
  XCircle,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { apiRequest } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import {
  ACTIVE_STATUSES,
  isTerminal,
  shouldPollTasks,
  STAGES,
  STAGE_LABELS,
  STATUS_LABELS,
  stageIndex,
} from "@/lib/task-state";
import type { ArtworkTask, TaskStatus } from "@/lib/types";

export function TaskHistoryPanel({
  userId,
  templateId,
  compact = false,
}: {
  userId: string;
  templateId?: string;
  compact?: boolean;
}) {
  const [previewTask, setPreviewTask] = useState<ArtworkTask | null>(null);
  const tasksQuery = useQuery({
    queryKey: queryKeys.tasks(userId),
    queryFn: ({ signal }) =>
      apiRequest<ArtworkTask[]>("/api/tasks", { signal }),
    refetchInterval: (query) =>
      shouldPollTasks(query.state.data as ArtworkTask[] | undefined)
        ? 2000
        : false,
    refetchIntervalInBackground: true,
  });

  const allTasks = tasksQuery.data ?? [];
  const tasks = templateId
    ? allTasks.filter((task) => task.templateId === templateId)
    : allTasks;
  const activeCount = allTasks.filter((task) =>
    ACTIVE_STATUSES.includes(task.status),
  ).length;

  return (
    <section
      className={compact ? "task-panel task-panel-compact" : "task-panel"}
      aria-labelledby={compact ? "template-history-title" : "history-title"}
    >
      <div className="section-heading task-section-heading">
        <div>
          <p className="eyebrow">PERSONAL ARCHIVE</p>
          <h2 id={compact ? "template-history-title" : "history-title"}>
            {compact ? "本模板制作记录" : "我的制作"}
          </h2>
        </div>
        <div className="history-actions">
          {activeCount > 0 ? (
            <span className="live-indicator">
              <span className="live-dot" />
              {activeCount} 个任务进行中
            </span>
          ) : null}
          <button
            type="button"
            className="icon-button"
            aria-label="刷新制作记录"
            title="刷新制作记录"
            onClick={() => void tasksQuery.refetch()}
            disabled={tasksQuery.isFetching}
          >
            <RefreshCw
              size={17}
              className={tasksQuery.isFetching ? "spin" : ""}
            />
          </button>
        </div>
      </div>

      {tasksQuery.isError ? (
        <div className="query-warning" role="alert">
          <AlertCircle size={18} />
          <div>
            <strong>查询暂时不可用</strong>
            <p>
              {tasks.length > 0
                ? "已保留上次获取的任务状态，后台任务仍可能继续运行。"
                : "暂时无法获取个人制作记录，这不代表任务执行失败。"}
            </p>
          </div>
          <button
            className="text-button"
            type="button"
            onClick={() => void tasksQuery.refetch()}
          >
            恢复查询
          </button>
        </div>
      ) : null}

      {tasksQuery.isPending ? (
        <div className="task-loading-grid" aria-label="正在加载制作记录">
          <TaskSkeleton />
          <TaskSkeleton />
        </div>
      ) : tasks.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">
            <ImageIcon size={24} />
          </span>
          <h3>{compact ? "还没有为这个模板创作" : "你的作品正在等待第一步"}</h3>
          <p>
            {compact
              ? "准备好素材后提交，任务会在这里持续更新。"
              : "从模板库挑选一个开始，提交后可以继续浏览和创建更多任务。"}
          </p>
          {!compact ? (
            <Link className="secondary-button" href="/">
              浏览模板
              <ArrowUpRight size={16} />
            </Link>
          ) : null}
        </div>
      ) : (
        <div className={compact ? "task-grid compact-grid" : "task-grid"}>
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onPreview={() => setPreviewTask(task)}
            />
          ))}
        </div>
      )}

      {previewTask?.resultUrl ? (
        <ResultDialog
          task={previewTask}
          onClose={() => setPreviewTask(null)}
        />
      ) : null}
    </section>
  );
}

export function TaskCard({
  task,
  onPreview,
}: {
  task: ArtworkTask;
  onPreview: () => void;
}) {
  const currentStage = stageIndex(task.stage);
  return (
    <article
      className={`task-card status-${task.status.toLowerCase()}`}
      aria-label={`${task.templateTitle}，${STATUS_LABELS[task.status]}`}
      data-testid={`task-${task.id}`}
    >
      <div className="task-card-media">
        {task.status === "SUCCEEDED" && task.resultUrl ? (
          // The result endpoint checks the signed session before serving this image.
          // eslint-disable-next-line @next/next/no-img-element
          <img src={task.resultUrl} alt={`${task.templateTitle}制作结果`} />
        ) : (
          <div className={`task-placeholder placeholder-${task.status.toLowerCase()}`}>
            <TaskStatusIcon status={task.status} />
            <span>{task.stage ? STAGE_LABELS[task.stage] : "正在进入队列"}</span>
            {task.status === "WAITING" || task.status === "PROCESSING" ? (
              <span className="processing-line" />
            ) : null}
          </div>
        )}
        {task.status === "SUCCEEDED" ? (
          <button
            type="button"
            className="preview-result-button"
            onClick={onPreview}
            aria-label={`预览${task.templateTitle}结果`}
          >
            <ArrowUpRight size={17} />
          </button>
        ) : null}
      </div>
      <div className="task-card-body">
        <div className="task-title-row">
          <h3>{task.templateTitle}</h3>
          <StatusPill status={task.status} />
        </div>
        <p className="task-time">
          <Clock3 size={14} />
          {formatDate(task.createdAt)}
        </p>

        {!isTerminal(task) ? (
          <div className="stage-track" aria-label="任务执行阶段">
            {STAGES.map((stage, index) => {
              const completed =
                task.status === "SUCCEEDED" || currentStage > index;
              const current =
                task.status === "PROCESSING" && currentStage === index;
              return (
                <div
                  key={stage.id}
                  className={`stage-step ${completed ? "completed" : ""} ${current ? "current" : ""}`}
                >
                  <span className="stage-node">
                    {completed ? <CheckCircle2 size={13} /> : index + 1}
                  </span>
                  <span>{stage.shortLabel}</span>
                </div>
              );
            })}
          </div>
        ) : null}

        {task.status === "WAITING" ? (
          <p className="task-note">排队中，后台会自动开始处理。</p>
        ) : null}
        {task.status === "PROCESSING" ? (
          <p className="task-note">
            当前：{task.stage ? STAGE_LABELS[task.stage] : "准备处理"}
          </p>
        ) : null}
        {task.status === "FAILED" ? (
          <p className="task-failure">
            <XCircle size={15} />
            {task.failureReason || "制作阶段执行失败"}
          </p>
        ) : null}
        {task.status === "SUCCEEDED" ? (
          <p className="task-success">
            <Sparkles size={15} />
            结果已保存，可随时回来查看
          </p>
        ) : null}
      </div>
    </article>
  );
}

function StatusPill({ status }: { status: TaskStatus }) {
  const Icon =
    status === "SUCCEEDED"
      ? CheckCircle2
      : status === "FAILED"
        ? XCircle
        : status === "PROCESSING"
          ? LoaderCircle
          : Clock3;
  return (
    <span className={`status-pill ${status.toLowerCase()}`}>
      <Icon size={13} className={status === "PROCESSING" ? "spin" : ""} />
      {STATUS_LABELS[status]}
    </span>
  );
}

function TaskStatusIcon({ status }: { status: TaskStatus }) {
  if (status === "FAILED") {
    return <XCircle size={24} />;
  }
  if (status === "WAITING") {
    return <Clock3 size={24} />;
  }
  return <LoaderCircle className="spin" size={24} />;
}

function TaskSkeleton() {
  return (
    <div className="task-card skeleton-card" aria-hidden="true">
      <div className="skeleton-block skeleton-media" />
      <div className="task-card-body">
        <span className="skeleton-line wide" />
        <span className="skeleton-line narrow" />
        <span className="skeleton-line medium" />
      </div>
    </div>
  );
}

function ResultDialog({
  task,
  onClose,
}: {
  task: ArtworkTask;
  onClose: () => void;
}) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="modal-layer">
      <button
        className="modal-backdrop"
        onClick={onClose}
        aria-label="关闭结果预览"
        type="button"
      />
      <div
        className="result-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="result-title"
      >
        <div className="result-dialog-heading">
          <div>
            <p className="eyebrow">GENERATED RESULT</p>
            <h2 id="result-title">{task.templateTitle}</h2>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={onClose}
            aria-label="关闭结果预览"
          >
            <span aria-hidden="true">×</span>
          </button>
        </div>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          className="result-full-image"
          src={task.resultUrl}
          alt={`${task.templateTitle}最终制作结果`}
        />
      </div>
    </div>
  );
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
