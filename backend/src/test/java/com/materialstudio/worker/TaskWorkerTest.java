package com.materialstudio.worker;

import com.materialstudio.domain.Asset;
import com.materialstudio.domain.CreationTask;
import com.materialstudio.domain.TaskStage;
import com.materialstudio.domain.TaskStatus;
import com.materialstudio.domain.Template;
import com.materialstudio.mapper.AssetMapper;
import com.materialstudio.mapper.TaskAssetMapper;
import com.materialstudio.mapper.TaskMapper;
import com.materialstudio.mapper.TemplateMapper;
import com.materialstudio.service.ResultRenderer;
import com.materialstudio.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskWorkerTest {
    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final TaskAssetMapper taskAssetMapper = mock(TaskAssetMapper.class);
    private final AssetMapper assetMapper = mock(AssetMapper.class);
    private final TemplateMapper templateMapper = mock(TemplateMapper.class);
    private final StorageService storageService = mock(StorageService.class);
    private final ResultRenderer resultRenderer = mock(ResultRenderer.class);
    private final TaskWorker worker = new TaskWorker(
            taskMapper,
            taskAssetMapper,
            assetMapper,
            templateMapper,
            storageService,
            resultRenderer
    );

    @Test
    void executesGenerationBeforeAssemblyAndOnlyThenMarksSuccess(@TempDir Path tempDir)
            throws Exception {
        Path imagePath = createImage(tempDir.resolve("input.png"));
        Path temporaryResult = tempDir.resolve("result.tmp");
        CreationTask task = task(null);
        Template template = template();
        Asset asset = asset();

        when(taskMapper.findById(task.getId())).thenReturn(task);
        when(taskMapper.claim(task.getId())).thenReturn(1);
        when(templateMapper.findById(template.getId())).thenReturn(template);
        when(taskAssetMapper.findAssetIds(task.getId())).thenReturn(List.of(asset.getId()));
        when(assetMapper.findByIdAndUser(asset.getId(), task.getUserId())).thenReturn(asset);
        when(storageService.resolveAsset(asset.getStorageName())).thenReturn(imagePath);
        when(storageService.createTemporaryResult(task.getId())).thenReturn(temporaryResult);
        when(storageService.commitResult(task.getId(), temporaryResult)).thenReturn("result.png");
        when(taskMapper.updateStage(task.getId(), TaskStage.GENERATION)).thenReturn(1);
        when(taskMapper.updateStage(task.getId(), TaskStage.ASSEMBLY)).thenReturn(1);
        when(taskMapper.markSucceeded(task.getId(), "result.png")).thenReturn(1);

        worker.run(task.getId());

        InOrder order = inOrder(
                taskMapper,
                storageService,
                resultRenderer
        );
        order.verify(taskMapper).claim(task.getId());
        order.verify(taskMapper).updateStage(task.getId(), TaskStage.GENERATION);
        order.verify(storageService).createTemporaryResult(task.getId());
        order.verify(resultRenderer).render(
                eq(temporaryResult),
                eq(template),
                any()
        );
        order.verify(taskMapper).updateStage(task.getId(), TaskStage.ASSEMBLY);
        order.verify(storageService).commitResult(task.getId(), temporaryResult);
        order.verify(taskMapper).markSucceeded(task.getId(), "result.png");
    }

    @Test
    void generationFailureNeverStartsAssembly(@TempDir Path tempDir) throws Exception {
        Path imagePath = createImage(tempDir.resolve("input.png"));
        CreationTask task = task(TaskStage.GENERATION);
        Template template = template();
        Asset asset = asset();

        when(taskMapper.findById(task.getId())).thenReturn(task);
        when(taskMapper.claim(task.getId())).thenReturn(1);
        when(templateMapper.findById(template.getId())).thenReturn(template);
        when(taskAssetMapper.findAssetIds(task.getId())).thenReturn(List.of(asset.getId()));
        when(assetMapper.findByIdAndUser(asset.getId(), task.getUserId())).thenReturn(asset);
        when(storageService.resolveAsset(asset.getStorageName())).thenReturn(imagePath);
        when(taskMapper.updateStage(task.getId(), TaskStage.GENERATION)).thenReturn(1);

        worker.run(task.getId());

        verify(taskMapper, never()).updateStage(task.getId(), TaskStage.ASSEMBLY);
        verify(storageService, never()).commitResult(eq(task.getId()), any());
        verify(taskMapper, never()).markSucceeded(eq(task.getId()), any());
        verify(taskMapper).markFailed(
                eq(task.getId()),
                eq(TaskStage.GENERATION),
                any()
        );
    }

    private CreationTask task(TaskStage failStage) {
        CreationTask task = new CreationTask();
        task.setId("task-id");
        task.setUserId("user-id");
        task.setTemplateId("template-id");
        task.setStatus(TaskStatus.WAITING);
        task.setMockDelayMs(0);
        task.setMockFailStage(failStage);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private Template template() {
        Template template = new Template();
        template.setId("template-id");
        template.setTitle("Test template");
        template.setCategory("Test");
        template.setDescription("Test description");
        template.setRequiredImageCount(1);
        template.setAcceptedMimeTypes("image/png");
        template.setMaxFileSizeBytes(1024 * 1024);
        return template;
    }

    private Asset asset() {
        Asset asset = new Asset();
        asset.setId("asset-id");
        asset.setUserId("user-id");
        asset.setStorageName("input.png");
        asset.setOriginalName("input.png");
        asset.setMimeType("image/png");
        return asset;
    }

    private Path createImage(Path path) throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, 16, 16);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
        return path;
    }
}
