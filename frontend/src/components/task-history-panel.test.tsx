import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TaskHistoryPanel } from "@/components/task-history-panel";
import { apiRequest } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import type { ArtworkTask } from "@/lib/types";

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    apiRequest: vi.fn(),
  };
});

const mockedApiRequest = vi.mocked(apiRequest);

afterEach(() => {
  cleanup();
  mockedApiRequest.mockReset();
});

describe("TaskHistoryPanel", () => {
  it("renders multiple tasks with independent stages and failures", async () => {
    mockedApiRequest.mockResolvedValueOnce([
      task({
        id: "task-running",
        status: "PROCESSING",
        stage: "GENERATION",
      }),
      task({
        id: "task-failed",
        status: "FAILED",
        stage: "ASSEMBLY",
        failedStage: "ASSEMBLY",
        failureReason: "结果整理失败，后续步骤已停止",
      }),
    ]);
    const queryClient = client();

    render(
      <QueryClientProvider client={queryClient}>
        <TaskHistoryPanel userId="demo-lin" compact />
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId("task-task-running")).toBeInTheDocument();
    expect(screen.getByTestId("task-task-failed")).toBeInTheDocument();
    expect(screen.getByText("处理中")).toBeInTheDocument();
    expect(screen.getByText("制作失败")).toBeInTheDocument();
    expect(
      screen.getByText("结果整理失败，后续步骤已停止"),
    ).toBeInTheDocument();
    expect(screen.queryByText("查询暂时不可用")).not.toBeInTheDocument();
  });

  it("preserves task data when polling fails and distinguishes the request error", async () => {
    const cached = [
      task({
        id: "cached-processing",
        status: "PROCESSING",
        stage: "VALIDATION",
      }),
    ];
    mockedApiRequest.mockRejectedValueOnce(new Error("network unavailable"));
    const queryClient = client();
    queryClient.setQueryData(queryKeys.tasks("demo-lin"), cached);

    render(
      <QueryClientProvider client={queryClient}>
        <TaskHistoryPanel userId="demo-lin" />
      </QueryClientProvider>,
    );

    expect(
      screen.getByTestId("task-cached-processing"),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText("查询暂时不可用")).toBeInTheDocument();
    });
    expect(screen.getByText(/已保留上次获取的任务状态/)).toBeInTheDocument();
    expect(screen.queryByText("制作失败")).not.toBeInTheDocument();
    expect(
      screen.getByText("处理中", { selector: ".status-pill" }),
    ).toBeInTheDocument();
  });
});

function client() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        staleTime: 0,
        gcTime: 0,
      },
    },
  });
}

function task(overrides: Partial<ArtworkTask>): ArtworkTask {
  return {
    id: "task-id",
    templateId: "coffee-poster",
    templateTitle: "咖啡时刻",
    coverUrl: "/api/media/templates/coffee.svg",
    assetCount: 1,
    status: "WAITING",
    createdAt: "2026-10-31T12:00:00+08:00",
    updatedAt: "2026-10-31T12:00:00+08:00",
    ...overrides,
  };
}
