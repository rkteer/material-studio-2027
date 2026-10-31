package com.materialstudio.dto;

import com.materialstudio.domain.CreationTask;
import com.materialstudio.domain.TaskStage;
import com.materialstudio.domain.TaskStatus;
import com.materialstudio.domain.Template;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record TaskView(
        String id,
        String templateId,
        String templateTitle,
        String coverUrl,
        int assetCount,
        TaskStatus status,
        TaskStage stage,
        TaskStage failedStage,
        String failureReason,
        String resultUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    public static TaskView from(CreationTask task, Template template, int assetCount) {
        String resultUrl = task.getStatus() == TaskStatus.SUCCEEDED
                ? "/api/tasks/" + task.getId() + "/result"
                : null;
        return new TaskView(
                task.getId(),
                task.getTemplateId(),
                template.getTitle(),
                template.getCoverUrl(),
                assetCount,
                task.getStatus(),
                task.getStage(),
                task.getFailedStage(),
                task.getFailureReason(),
                resultUrl,
                task.getCreatedAt().atZone(DISPLAY_ZONE).toOffsetDateTime(),
                task.getUpdatedAt().atZone(DISPLAY_ZONE).toOffsetDateTime()
        );
    }
}
