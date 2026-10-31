package com.materialstudio.worker;

import com.materialstudio.domain.Asset;
import com.materialstudio.domain.CreationTask;
import com.materialstudio.domain.TaskStage;
import com.materialstudio.domain.Template;
import com.materialstudio.mapper.AssetMapper;
import com.materialstudio.mapper.TaskAssetMapper;
import com.materialstudio.mapper.TaskMapper;
import com.materialstudio.mapper.TemplateMapper;
import com.materialstudio.service.ResultRenderer;
import com.materialstudio.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class TaskWorker {
    private static final Logger log = LoggerFactory.getLogger(TaskWorker.class);

    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final AssetMapper assetMapper;
    private final TemplateMapper templateMapper;
    private final StorageService storageService;
    private final ResultRenderer resultRenderer;

    public TaskWorker(
            TaskMapper taskMapper,
            TaskAssetMapper taskAssetMapper,
            AssetMapper assetMapper,
            TemplateMapper templateMapper,
            StorageService storageService,
            ResultRenderer resultRenderer
    ) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.assetMapper = assetMapper;
        this.templateMapper = templateMapper;
        this.storageService = storageService;
        this.resultRenderer = resultRenderer;
    }

    public void run(String taskId) {
        CreationTask task = taskMapper.findById(taskId);
        if (task == null || taskMapper.claim(taskId) == 0) {
            return;
        }

        TaskStage currentStage = TaskStage.VALIDATION;
        Path temporaryResult = null;
        try {
            simulate(task);
            if (isForcedFailure(task, currentStage)) {
                throw new ForcedSimulationFailure(currentStage);
            }
            Template template = requiredTemplate(task);
            List<Asset> assets = validateAssets(task, template);

            currentStage = TaskStage.GENERATION;
            if (taskMapper.updateStage(taskId, currentStage) == 0) {
                return;
            }
            simulate(task);
            if (isForcedFailure(task, currentStage)) {
                throw new ForcedSimulationFailure(currentStage);
            }
            temporaryResult = storageService.createTemporaryResult(taskId);
            resultRenderer.render(temporaryResult, template, assets);

            currentStage = TaskStage.ASSEMBLY;
            if (taskMapper.updateStage(taskId, currentStage) == 0) {
                return;
            }
            simulate(task);
            if (isForcedFailure(task, currentStage)) {
                throw new ForcedSimulationFailure(currentStage);
            }
            String resultName = storageService.commitResult(taskId, temporaryResult);
            temporaryResult = null;
            if (taskMapper.markSucceeded(taskId, resultName) == 0) {
                storageService.deleteResult(resultName);
            }
        } catch (ForcedSimulationFailure failure) {
            fail(taskId, failure.stage, "已触发 " + failure.stage + " 阶段模拟失败，后续步骤已停止");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail(taskId, currentStage, "后台处理被中断，任务未完成");
        } catch (Exception exception) {
            log.error("Task {} failed during {}", taskId, currentStage, exception);
            fail(taskId, currentStage, readableFailure(exception));
        } finally {
            if (temporaryResult != null) {
                storageService.deleteTemporaryResult(temporaryResult);
            }
        }
    }

    private List<Asset> validateAssets(CreationTask task, Template template) throws Exception {
        List<String> assetIds = taskAssetMapper.findAssetIds(task.getId());
        if (assetIds.size() != template.getRequiredImageCount()) {
            throw new IllegalStateException("素材数量不符合模板要求");
        }
        List<Asset> assets = assetIds.stream()
                .map(id -> assetMapper.findByIdAndUser(id, task.getUserId()))
                .toList();
        if (assets.stream().anyMatch(asset -> asset == null)) {
            throw new IllegalStateException("部分素材已不存在");
        }
        for (Asset asset : assets) {
            Path path = storageService.resolveAsset(asset.getStorageName());
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IllegalStateException("素材无法解码");
            }
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("素材文件缺失");
            }
        }
        return assets;
    }

    private Template requiredTemplate(CreationTask task) {
        Template template = templateMapper.findById(task.getTemplateId());
        if (template == null) {
            throw new IllegalStateException("模板已下线");
        }
        return template;
    }

    private void simulate(CreationTask task) throws InterruptedException {
        if (task.getMockDelayMs() > 0) {
            Thread.sleep(task.getMockDelayMs());
        }
    }

    private boolean isForcedFailure(CreationTask task, TaskStage stage) {
        return task.getMockFailStage() == stage;
    }

    private void fail(String taskId, TaskStage stage, String reason) {
        String safeReason = reason == null || reason.isBlank()
                ? "制作过程中发生异常"
                : reason.substring(0, Math.min(reason.length(), 1000));
        taskMapper.markFailed(taskId, stage, safeReason);
    }

    private String readableFailure(Exception exception) {
        if (exception instanceof IllegalStateException
                || exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        return "图片处理失败，请确认素材完整后重新创建任务";
    }

    private static class ForcedSimulationFailure extends RuntimeException {
        private final TaskStage stage;

        private ForcedSimulationFailure(TaskStage stage) {
            this.stage = stage;
        }
    }
}
