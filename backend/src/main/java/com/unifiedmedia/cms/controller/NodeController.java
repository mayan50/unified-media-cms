package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.service.NodeRegistryService;
import com.unifiedmedia.cms.service.NodeRegistryService.CategoryDef;
import com.unifiedmedia.cms.service.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final PipelineOrchestrator orchestrator;
    private final NodeRegistryService nodeRegistryService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAvailableNodes() {
        return ResponseEntity.ok(orchestrator.getAvailableNodes());
    }

    @GetMapping("/registry")
    public ResponseEntity<List<CategoryDef>> getNodeRegistry() {
        return ResponseEntity.ok(nodeRegistryService.getRegistry());
    }
}
