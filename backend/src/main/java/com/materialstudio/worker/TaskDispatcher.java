package com.materialstudio.worker;

import com.materialstudio.domain.TaskStage;
import com.materialstudio.mapper.TaskMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Component
public class TaskDispatcher {
    private static final Logger log = LoggerFactory.getLogger(TaskDispatcher.class);

    private final ExecutorService executor;
    private final TaskWorker taskWorker;
    private final TaskMapper taskMapper;

    public TaskDispatcher(
            @Qualifier("taskWorkerExecutor") ExecutorService executor,
            TaskWorker taskWorker,
            TaskMapper taskMapper
    ) {
        this.executor = executor;
        this.taskWorker = taskWorker;
        this.taskMapper = taskMapper;
    }

    public void submit(String taskId) {
        try {
            executor.execute(() -> taskWorker.run(taskId));
        } catch (RejectedExecutionException exception) {
            log.warn("Task queue rejected {}", taskId);
            taskMapper.markFailed(taskId, null, "系统繁忙，任务未能进入后台队列");
        }
    }

    @PreDestroy
    void stopAcceptingTasks() {
        executor.shutdown();
    }
}
