import { describe, expect, it } from "vitest";
import {
  isTerminal,
  shouldPollTasks,
  stageIndex,
} from "@/lib/task-state";
import type { ArtworkTask, TaskStatus } from "@/lib/types";

function task(
  id: string,
  status: TaskStatus,
  stage?: ArtworkTask["stage"],
): ArtworkTask {
  return {
    id,
    templateId: "coffee-poster",
    templateTitle: "咖啡时刻",
    coverUrl: "/api/media/templates/coffee.svg",
    assetCount: 1,
    status,
    stage,
    createdAt: "2026-10-31T12:00:00+08:00",
    updatedAt: "2026-10-31T12:00:01+08:00",
  };
}

describe("task state helpers", () => {
  it("polls while any task is waiting or processing", () => {
    expect(
      shouldPollTasks([
        task("done", "SUCCEEDED", "COMPLETED"),
        task("running", "PROCESSING", "GENERATION"),
      ]),
    ).toBe(true);
    expect(
      shouldPollTasks([
        task("first", "FAILED", "GENERATION"),
        task("second", "SUCCEEDED", "COMPLETED"),
      ]),
    ).toBe(false);
    expect(shouldPollTasks(undefined)).toBe(false);
  });

  it("keeps terminal statuses and stages independent", () => {
    expect(isTerminal(task("failed", "FAILED", "ASSEMBLY"))).toBe(true);
    expect(isTerminal(task("running", "PROCESSING", "VALIDATION"))).toBe(false);
    expect(stageIndex("VALIDATION")).toBe(0);
    expect(stageIndex("GENERATION")).toBe(1);
    expect(stageIndex("ASSEMBLY")).toBe(2);
    expect(stageIndex("COMPLETED")).toBe(3);
  });
});
