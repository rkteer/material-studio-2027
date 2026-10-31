package com.materialstudio.dto;

import java.util.List;

public record TemplateListResponse(List<TemplateView> templates, List<String> categories) {
}
