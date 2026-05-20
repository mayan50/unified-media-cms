package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.TemplateRequest;
import com.unifiedmedia.cms.entity.Template;
import com.unifiedmedia.cms.service.TemplateService;
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

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<Template>> listTemplates() {
        return ResponseEntity.ok(templateService.listTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Template> getTemplate(@PathVariable UUID id) {
        return templateService.getTemplate(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Template> createTemplate(@Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.createTemplate(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Template> updateTemplate(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
