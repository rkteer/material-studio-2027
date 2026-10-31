package com.materialstudio.service;

import com.materialstudio.domain.TaskStage;
import com.materialstudio.dto.SimulationSettings;
import com.materialstudio.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulationService {
    private final AtomicReference<SimulationSettings> current;

    public SimulationService(
            @Value("${app.simulation.default-delay-ms}") long defaultDelayMs,
            @Value("${app.simulation.default-fail-stage:}") String defaultFailStage
    ) {
        TaskStage stage = defaultFailStage == null || defaultFailStage.isBlank()
                ? null
                : TaskStage.valueOf(defaultFailStage.trim().toUpperCase());
        this.current = new AtomicReference<>(new SimulationSettings(defaultDelayMs, stage));
    }

    public SimulationSettings get() {
        return current.get();
    }

    public SimulationSettings update(SimulationSettings settings) {
        if (settings == null) {
            throw invalid("模拟配置不能为空");
        }
        if (settings.stepDelayMs() < 0 || settings.stepDelayMs() > 60_000) {
            throw invalid("每步耗时必须在 0 到 60000 毫秒之间");
        }
        if (settings.failStage() == TaskStage.COMPLETED) {
            throw invalid("不能把 COMPLETED 设为失败阶段");
        }
        SimulationSettings normalized = new SimulationSettings(
                settings.stepDelayMs(),
                settings.failStage()
        );
        current.set(normalized);
        return normalized;
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIMULATION", message);
    }
}
