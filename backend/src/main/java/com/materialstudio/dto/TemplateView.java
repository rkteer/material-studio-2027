package com.materialstudio.dto;

import com.materialstudio.domain.Template;

import java.util.Arrays;
import java.util.List;

public record TemplateView(
        String id,
        String title,
        String category,
        String coverUrl,
        String description,
        int requiredImageCount,
        List<String> acceptedMimeTypes,
        long maxFileSizeBytes
) {
    public static TemplateView from(Template template) {
        return new TemplateView(
                template.getId(),
                template.getTitle(),
                template.getCategory(),
                template.getCoverUrl(),
                template.getDescription(),
                template.getRequiredImageCount(),
                Arrays.stream(template.getAcceptedMimeTypes().split(",")).map(String::trim).toList(),
                template.getMaxFileSizeBytes()
        );
    }
}
