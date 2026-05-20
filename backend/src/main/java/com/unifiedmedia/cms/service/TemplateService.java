package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.dto.TemplateRequest;
import com.unifiedmedia.cms.entity.Template;
import com.unifiedmedia.cms.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<Template> listTemplates() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Template> getTemplate(UUID id) {
        return templateRepository.findById(id);
    }

    @Transactional
    public Template createTemplate(TemplateRequest request) {
        Template template = Template.builder()
                .name(request.getName())
                .description(request.getDescription())
                .graphPayload(request.getGraphPayload())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
        return templateRepository.save(template);
    }

    @Transactional
    public Template updateTemplate(UUID id, TemplateRequest request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setGraphPayload(request.getGraphPayload());
        if (request.getIsDefault() != null) template.setIsDefault(request.getIsDefault());
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        templateRepository.deleteById(id);
    }
}
