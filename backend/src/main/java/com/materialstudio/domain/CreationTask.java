package com.materialstudio.domain;

import java.time.LocalDateTime;

public class CreationTask {
    private String id;
    private String userId;
    private String templateId;
    private TaskStatus status;
    private TaskStage stage;
    private TaskStage failedStage;
    private String failureReason;
    private String resultStorageName;
    private long mockDelayMs;
    private TaskStage mockFailStage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskStage getStage() {
        return stage;
    }

    public void setStage(TaskStage stage) {
        this.stage = stage;
    }

    public TaskStage getFailedStage() {
        return failedStage;
    }

    public void setFailedStage(TaskStage failedStage) {
        this.failedStage = failedStage;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getResultStorageName() {
        return resultStorageName;
    }

    public void setResultStorageName(String resultStorageName) {
        this.resultStorageName = resultStorageName;
    }

    public long getMockDelayMs() {
        return mockDelayMs;
    }

    public void setMockDelayMs(long mockDelayMs) {
        this.mockDelayMs = mockDelayMs;
    }

    public TaskStage getMockFailStage() {
        return mockFailStage;
    }

    public void setMockFailStage(TaskStage mockFailStage) {
        this.mockFailStage = mockFailStage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
