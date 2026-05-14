package com.unifiedmedia.cms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.dto.TemplateRequest;
import com.unifiedmedia.cms.entity.Template;
import com.unifiedmedia.cms.repository.TemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Template>> listTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Template> getTemplate(@PathVariable UUID id) {
        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Template> createTemplate(@Valid @RequestBody TemplateRequest request) {
        Template template = Template.builder()
                .name(request.getName())
                .description(request.getDescription())
                .graphPayload(serializeJson(request.getGraphPayload()))
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Template> updateTemplate(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setGraphPayload(serializeJson(request.getGraphPayload()));
        if (request.getIsDefault() != null) template.setIsDefault(request.getIsDefault());
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String serializeJson(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
