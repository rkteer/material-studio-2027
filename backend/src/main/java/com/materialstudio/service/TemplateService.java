package com.materialstudio.service;

import com.materialstudio.domain.Template;
import com.materialstudio.dto.TemplateListResponse;
import com.materialstudio.dto.TemplateView;
import com.materialstudio.error.ApiException;
import com.materialstudio.mapper.TemplateMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {
    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public TemplateListResponse list(String category) {
        List<Template> templates = category == null || category.isBlank()
                ? templateMapper.findAll()
                : templateMapper.findByCategory(category.trim());
        return new TemplateListResponse(
                templates.stream().map(TemplateView::from).toList(),
                templateMapper.findCategories()
        );
    }

    public TemplateView get(String id) {
        return TemplateView.from(requireTemplate(id));
    }

    public Template requireTemplate(String id) {
        Template template = templateMapper.findById(id);
        if (template == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "模板不存在或已下线");
        }
        return template;
    }
}
