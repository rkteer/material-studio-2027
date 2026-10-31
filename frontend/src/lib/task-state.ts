import type { ArtworkTask, TaskStage, TaskStatus } from "@/lib/types";

export const ACTIVE_STATUSES: TaskStatus[] = ["WAITING", "PROCESSING"];

export const STAGES: Array<{
  id: TaskStage;
  label: string;
  shortLabel: string;
}> = [
  { id: "VALIDATION", label: "素材检查", shortLabel: "检查" },
  { id: "GENERATION", label: "模拟生成", shortLabel: "生成" },
  { id: "ASSEMBLY", label: "结果整理", shortLabel: "整理" },
];

export const STATUS_LABELS: Record<TaskStatus, string> = {
  WAITING: "等待处理",
  PROCESSING: "处理中",
  SUCCEEDED: "制作完成",
  FAILED: "制作失败",
};

export const STAGE_LABELS: Record<TaskStage, string> = {
  VALIDATION: "素材检查",
  GENERATION: "模拟生成",
  ASSEMBLY: "结果整理",
  COMPLETED: "已完成",
};

export function shouldPollTasks(tasks: ArtworkTask[] | undefined): boolean {
  return Boolean(tasks?.some((task) => ACTIVE_STATUSES.includes(task.status)));
}

export function stageIndex(stage?: TaskStage): number {
  if (!stage || stage === "COMPLETED") {
    return stage === "COMPLETED" ? STAGES.length : -1;
  }
  return STAGES.findIndex((item) => item.id === stage);
}

export function isTerminal(task: ArtworkTask): boolean {
  return task.status === "SUCCEEDED" || task.status === "FAILED";
}
