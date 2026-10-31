package com.materialstudio.service;

import com.materialstudio.domain.Asset;
import com.materialstudio.domain.CreationTask;
import com.materialstudio.domain.TaskStatus;
import com.materialstudio.domain.Template;
import com.materialstudio.dto.CreateTaskRequest;
import com.materialstudio.dto.SimulationSettings;
import com.materialstudio.dto.TaskView;
import com.materialstudio.error.ApiException;
import com.materialstudio.mapper.AssetMapper;
import com.materialstudio.mapper.TaskAssetMapper;
import com.materialstudio.mapper.TaskMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final AssetMapper assetMapper;
    private final TemplateService templateService;
    private final SimulationService simulationService;
    private final StorageService storageService;

    public TaskService(
            TaskMapper taskMapper,
            TaskAssetMapper taskAssetMapper,
            AssetMapper assetMapper,
            TemplateService templateService,
            SimulationService simulationService,
            StorageService storageService
    ) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.assetMapper = assetMapper;
        this.templateService = templateService;
        this.simulationService = simulationService;
        this.storageService = storageService;
    }

    @Transactional
    public TaskView create(String userId, CreateTaskRequest request) {
        if (request == null || request.templateId() == null || request.templateId().isBlank()) {
            throw badRequest("TEMPLATE_REQUIRED", "请选择一个模板");
        }
        Template template = templateService.requireTemplate(request.templateId());
        List<String> assetIds = request.assetIds();
        if (assetIds == null || assetIds.size() != template.getRequiredImageCount()) {
            throw badRequest(
                    "INVALID_ASSET_COUNT",
                    "该模板需要 " + template.getRequiredImageCount() + " 张图片"
            );
        }

        Set<String> uniqueIds = new HashSet<>(assetIds);
        if (uniqueIds.size() != assetIds.size() || assetIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw badRequest("DUPLICATE_ASSET", "同一个素材不能重复使用");
        }

        Map<String, Asset> ownedAssets = assetIds.stream()
                .map(id -> assetMapper.findByIdAndUser(id, userId))
                .filter(asset -> asset != null)
                .collect(Collectors.toMap(Asset::getId, Function.identity()));
        if (ownedAssets.size() != assetIds.size()) {
            throw badRequest("ASSET_NOT_READY", "部分素材不存在或尚未上传完成");
        }

        List<String> acceptedTypes = Arrays.stream(template.getAcceptedMimeTypes().split(","))
                .map(String::trim)
                .toList();
        for (String assetId : assetIds) {
            Asset asset = ownedAssets.get(assetId);
            if (!acceptedTypes.contains(asset.getMimeType())) {
                throw badRequest("UNSUPPORTED_ASSET_TYPE", "素材格式不符合模板要求");
            }
            if (asset.getSizeBytes() > template.getMaxFileSizeBytes()) {
                throw badRequest("ASSET_TOO_LARGE", "有素材超过该模板的大小限制");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        SimulationSettings settings = simulationService.get();
        CreationTask task = new CreationTask();
        task.setId(UUID.randomUUID().toString());
        task.setUserId(userId);
        task.setTemplateId(template.getId());
        task.setStatus(TaskStatus.WAITING);
        task.setMockDelayMs(settings.stepDelayMs());
        task.setMockFailStage(settings.failStage());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);

        for (int index = 0; index < assetIds.size(); index++) {
            taskAssetMapper.insert(task.getId(), assetIds.get(index), index);
        }
        return TaskView.from(task, template, assetIds.size());
    }

    public List<TaskView> list(String userId) {
        return taskMapper.findAllByUser(userId).stream()
                .map(task -> TaskView.from(
                        task,
                        templateService.requireTemplate(task.getTemplateId()),
                        taskAssetMapper.countByTaskId(task.getId())
                ))
                .toList();
    }

    public TaskView get(String userId, String taskId) {
        CreationTask task = requireOwnedTask(userId, taskId);
        return TaskView.from(
                task,
                templateService.requireTemplate(task.getTemplateId()),
                taskAssetMapper.countByTaskId(task.getId())
        );
    }

    public CreationTask requireOwnedTask(String userId, String taskId) {
        CreationTask task = taskMapper.findByIdAndUser(taskId, userId);
        if (task == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "制作任务不存在");
        }
        return task;
    }

    public java.nio.file.Path resultPath(CreationTask task) {
        if (task.getStatus() != TaskStatus.SUCCEEDED || task.getResultStorageName() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESULT_NOT_READY", "制作结果尚未生成");
        }
        return storageService.resolveResult(task.getResultStorageName());
    }

    private ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
