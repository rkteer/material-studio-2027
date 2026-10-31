package com.materialstudio;

import com.materialstudio.domain.TaskStage;
import com.materialstudio.domain.TaskStatus;
import com.materialstudio.dto.CreateTaskRequest;
import com.materialstudio.dto.SimulationSettings;
import com.materialstudio.dto.TaskView;
import com.materialstudio.error.ApiException;
import com.materialstudio.mapper.TaskMapper;
import com.materialstudio.service.AssetService;
import com.materialstudio.service.SimulationService;
import com.materialstudio.service.TaskService;
import com.materialstudio.worker.TaskWorker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MaterialStudioIntegrationTest {
    private static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "material-studio-tests-" + UUID.randomUUID()
    );

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", () -> STORAGE_ROOT.toString());
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AssetService assetService;

    @Autowired
    TaskService taskService;

    @Autowired
    SimulationService simulationService;

    @Autowired
    TaskWorker taskWorker;

    @Autowired
    TaskMapper taskMapper;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM task_asset");
        jdbcTemplate.update("DELETE FROM creation_task");
        jdbcTemplate.update("DELETE FROM asset");
        simulationService.update(new SimulationSettings(0, null));
    }

    @AfterAll
    static void cleanStorage() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // The operating system can remove a locked test file later.
                        }
                    });
        }
    }

    @Test
    void successfulTaskRunsAllStagesAndQueriesDoNotChangeIt() {
        var asset = uploadPng("demo-lin");
        TaskView created = taskService.create(
                "demo-lin",
                new CreateTaskRequest("coffee-poster", List.of(asset.id()))
        );

        assertThat(created.status()).isEqualTo(TaskStatus.WAITING);
        assertThat(created.stage()).isNull();

        taskWorker.run(created.id());

        TaskView firstRead = taskService.get("demo-lin", created.id());
        TaskView secondRead = taskService.get("demo-lin", created.id());
        assertThat(firstRead.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(firstRead.stage()).isEqualTo(TaskStage.COMPLETED);
        assertThat(firstRead.resultUrl()).isEqualTo("/api/tasks/" + created.id() + "/result");
        assertThat(secondRead.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(taskService.resultPath(taskMapper.findById(created.id()))).exists();
    }

    @Test
    void forcedGenerationFailureStopsBeforeAssembly() {
        simulationService.update(new SimulationSettings(0, TaskStage.GENERATION));
        var asset = uploadPng("demo-lin");
        TaskView created = taskService.create(
                "demo-lin",
                new CreateTaskRequest("coffee-poster", List.of(asset.id()))
        );

        taskWorker.run(created.id());

        TaskView failed = taskService.get("demo-lin", created.id());
        assertThat(failed.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(failed.failedStage()).isEqualTo(TaskStage.GENERATION);
        assertThat(failed.stage()).isEqualTo(TaskStage.GENERATION);
        assertThat(failed.failureReason()).contains("GENERATION");
        assertThat(failed.resultUrl()).isNull();
    }

    @Test
    void forcedValidationFailureNeverEntersGeneration() {
        simulationService.update(new SimulationSettings(0, TaskStage.VALIDATION));
        var asset = uploadPng("demo-lin");
        TaskView created = taskService.create(
                "demo-lin",
                new CreateTaskRequest("coffee-poster", List.of(asset.id()))
        );

        taskWorker.run(created.id());

        TaskView failed = taskService.get("demo-lin", created.id());
        assertThat(failed.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(failed.failedStage()).isEqualTo(TaskStage.VALIDATION);
        assertThat(failed.stage()).isEqualTo(TaskStage.VALIDATION);
    }

    @Test
    void tasksAndAssetsAreIsolatedByUser() {
        var asset = uploadPng("demo-lin");
        TaskView task = taskService.create(
                "demo-lin",
                new CreateTaskRequest("coffee-poster", List.of(asset.id()))
        );

        assertThatThrownBy(() -> taskService.get("demo-chen", task.id()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不存在");
        assertThatThrownBy(() -> assetService.delete("demo-chen", asset.id()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不存在");
        assertThat(taskService.list("demo-chen")).isEmpty();
    }

    @Test
    void invalidContentAndIncorrectAssetCountAreRejected() {
        MockMultipartFile notAnImage = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not an image".getBytes()
        );

        assertThatThrownBy(() -> assetService.upload("demo-lin", notAnImage))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("JPEG 或 PNG");

        assertThatThrownBy(() -> taskService.create(
                "demo-lin",
                new CreateTaskRequest("travel-postcard", List.of())
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("需要 2 张图片");
    }

    @Test
    void multipleTasksKeepIndependentStates() {
        var assetOne = uploadPng("demo-lin");
        var assetTwo = uploadPng("demo-lin");
        TaskView first = taskService.create(
                "demo-lin",
                new CreateTaskRequest("coffee-poster", List.of(assetOne.id()))
        );
        TaskView second = taskService.create(
                "demo-lin",
                new CreateTaskRequest("coffee-poster", List.of(assetTwo.id()))
        );

        taskWorker.run(first.id());

        assertThat(taskService.get("demo-lin", first.id()).status())
                .isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(taskService.get("demo-lin", second.id()).status())
                .isEqualTo(TaskStatus.WAITING);
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    private com.materialstudio.dto.AssetView uploadPng(String userId) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                pngBytes()
        );
        return assetService.upload(userId, file);
    }

    private byte[] pngBytes() {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(33, 56, 45));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(182, 243, 74));
            graphics.fillOval(170, 90, 300, 300);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
