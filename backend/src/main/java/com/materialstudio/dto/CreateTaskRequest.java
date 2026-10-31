package com.materialstudio.dto;

import java.util.List;

public record CreateTaskRequest(String templateId, List<String> assetIds) {
}
