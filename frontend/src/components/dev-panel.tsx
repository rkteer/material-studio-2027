"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, FlaskConical, LoaderCircle, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { apiRequest } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import {
  STAGE_LABELS,
  STAGES,
} from "@/lib/task-state";
import type { SimulationSettings, TaskStage } from "@/lib/types";

export function DevPanel({
  userId,
  open,
  onClose,
}: {
  userId: string;
  open: boolean;
  onClose: () => void;
}) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  const simulationQuery = useQuery({
    queryKey: queryKeys.simulation(userId),
    queryFn: ({ signal }) =>
      apiRequest<SimulationSettings>("/api/dev/simulation", { signal }),
    enabled: open,
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    closeButtonRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose, open]);

  if (!open) {
    return null;
  }

  return (
    <div className="drawer-layer" role="presentation">
      <button
        className="drawer-backdrop"
        aria-label="关闭开发设置"
        onClick={onClose}
        type="button"
      />
      <aside
        className="dev-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="dev-title"
      >
        <div className="drawer-heading">
          <span className="drawer-icon">
            <FlaskConical size={20} />
          </span>
          <div>
            <p className="eyebrow">DEMO CONTROL</p>
            <h2 id="dev-title">后台模拟设置</h2>
          </div>
          <button
            ref={closeButtonRef}
            className="icon-button"
            onClick={onClose}
            aria-label="关闭设置"
            type="button"
          >
            <X size={19} />
          </button>
        </div>

        {simulationQuery.isPending ? (
          <div className="panel-loading">
            <LoaderCircle className="spin" size={20} />
            读取当前设置…
          </div>
        ) : simulationQuery.isError ? (
          <div className="inline-error">
            无法读取模拟设置。
            <button type="button" onClick={() => void simulationQuery.refetch()}>
              重试
            </button>
          </div>
        ) : (
          <SimulationForm
            key={`${simulationQuery.data.stepDelayMs}-${simulationQuery.data.failStage ?? "none"}`}
            userId={userId}
            initialSettings={simulationQuery.data}
          />
        )}
      </aside>
    </div>
  );
}

function SimulationForm({
  userId,
  initialSettings,
}: {
  userId: string;
  initialSettings: SimulationSettings;
}) {
  const queryClient = useQueryClient();
  const [delay, setDelay] = useState(initialSettings.stepDelayMs);
  const [failStage, setFailStage] = useState<TaskStage | "">(
    initialSettings.failStage ?? "",
  );
  const updateMutation = useMutation({
    mutationFn: () =>
      apiRequest<SimulationSettings>("/api/dev/simulation", {
        method: "PUT",
        body: JSON.stringify({
          stepDelayMs: delay,
          failStage: failStage || null,
        }),
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(queryKeys.simulation(userId), data);
    },
  });

  return (
    <form
      className="dev-form"
      onSubmit={(event) => {
        event.preventDefault();
        updateMutation.mutate();
      }}
    >
      <label className="field-label" htmlFor="step-delay">
        每步模拟耗时
        <span>{delay} ms</span>
      </label>
      <input
        id="step-delay"
        className="range-input"
        type="range"
        min={0}
        max={6000}
        step={200}
        value={delay}
        onChange={(event) => setDelay(Number(event.target.value))}
      />
      <div className="range-caption">
        <span>立即</span>
        <span>6 秒</span>
      </div>

      <label className="field-label" htmlFor="fail-stage">
        强制失败阶段
      </label>
      <select
        id="fail-stage"
        className="select-control"
        value={failStage}
        onChange={(event) =>
          setFailStage(event.target.value as TaskStage | "")
        }
      >
        <option value="">不模拟失败</option>
        {STAGES.map((stage) => (
          <option key={stage.id} value={stage.id}>
            {STAGE_LABELS[stage.id]}
          </option>
        ))}
      </select>

      <p className="panel-note">
        设置在提交时复制到新任务，不会改变已经在后台运行的任务。
      </p>
      {updateMutation.isError ? (
        <p className="form-error" role="alert">
          {updateMutation.error.message}
        </p>
      ) : null}
      {updateMutation.isSuccess ? (
        <p className="form-success" role="status">
          <Check size={15} /> 已应用到之后创建的任务
        </p>
      ) : null}
      <button
        className="primary-button full-width"
        type="submit"
        disabled={updateMutation.isPending}
      >
        {updateMutation.isPending ? (
          <LoaderCircle className="spin" size={17} />
        ) : null}
        保存模拟设置
      </button>
    </form>
  );
}
