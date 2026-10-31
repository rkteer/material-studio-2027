package com.materialstudio.dto;

import com.materialstudio.domain.TaskStage;

public record SimulationSettings(long stepDelayMs, TaskStage failStage) {
}
