package com.materialstudio.controller;

import com.materialstudio.domain.CreationTask;
import com.materialstudio.dto.CreateTaskRequest;
import com.materialstudio.dto.TaskView;
import com.materialstudio.security.SessionAuthFilter;
import com.materialstudio.service.TaskService;
import com.materialstudio.worker.TaskDispatcher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskDispatcher taskDispatcher;

    public TaskController(TaskService taskService, TaskDispatcher taskDispatcher) {
        this.taskService = taskService;
        this.taskDispatcher = taskDispatcher;
    }

    @PostMapping
    ResponseEntity<TaskView> create(
            @RequestAttribute(SessionAuthFilter.USER_ID_ATTRIBUTE) String userId,
            @RequestBody CreateTaskRequest request
    ) {
        TaskView task = taskService.create(userId, request);
        taskDispatcher.submit(task.id());
        return ResponseEntity.status(202).body(task);
    }

    @GetMapping
    List<TaskView> list(
            @RequestAttribute(SessionAuthFilter.USER_ID_ATTRIBUTE) String userId
    ) {
        return taskService.list(userId);
    }

    @GetMapping("/{id}")
    TaskView get(
            @RequestAttribute(SessionAuthFilter.USER_ID_ATTRIBUTE) String userId,
            @PathVariable String id
    ) {
        return taskService.get(userId, id);
    }

    @GetMapping("/{id}/result")
    ResponseEntity<Resource> result(
            @RequestAttribute(SessionAuthFilter.USER_ID_ATTRIBUTE) String userId,
            @PathVariable String id
    ) {
        CreationTask task = taskService.requireOwnedTask(userId, id);
        Path path = taskService.resultPath(task);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"material-result.png\"")
                .body(new FileSystemResource(path));
    }
}
