package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.TaskResumeRequest;
import com.unifiedmedia.cms.dto.TaskSubmitRequest;
import com.unifiedmedia.cms.entity.PipelineTask;
import com.unifiedmedia.cms.entity.PipelineTaskLog;
import com.unifiedmedia.cms.service.PipelineOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final PipelineOrchestrator orchestrator;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@Valid @RequestBody TaskSubmitRequest request) {
        PipelineTask task = orchestrator.submitTask(
                request.getName(),
                request.getTemplateId(),
                request.getInputPath(),
                request.getOutputPath()
        );
        return ResponseEntity.ok(Map.of(
                "taskId", task.getId(),
                "assetId", task.getAssetId(),
                "status", task.getCurrentStatus(),
                "executionGraph", task.getExecutionGraph() != null ? task.getExecutionGraph() : Map.of()
        ));
    }

    @PostMapping("/{taskId}/resume")
    public ResponseEntity<Map<String, Object>> resumeTask(
            @PathVariable UUID taskId,
            @RequestBody TaskResumeRequest request) {
        orchestrator.resumeTask(taskId, request.getMergedPayload());
        return ResponseEntity.ok(Map.of("taskId", taskId, "status", "RESUMED"));
    }

    @GetMapping
    public ResponseEntity<List<PipelineTask>> listTasks(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(orchestrator.getTasksByStatus(status));
        }
        return ResponseEntity.ok(orchestrator.getAllTasks());
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<PipelineTask> getTask(@PathVariable UUID taskId) {
        return orchestrator.getTask(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<PipelineTaskLog>> getTaskLogs(@PathVariable UUID taskId) {
        return ResponseEntity.ok(orchestrator.getTaskLogs(taskId));
    }

    @PostMapping("/{taskId}/start")
    public ResponseEntity<Map<String, Object>> startTask(@PathVariable UUID taskId) {
        orchestrator.startTask(taskId);
        return ResponseEntity.ok(Map.of("taskId", taskId, "status", "RUNNING"));
    }

    @PostMapping("/{taskId}/stop")
    public ResponseEntity<Map<String, Object>> stopTask(@PathVariable UUID taskId) {
        orchestrator.stopTask(taskId);
        return ResponseEntity.ok(Map.of("taskId", taskId, "status", "FAILED"));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        orchestrator.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<PipelineTask> updateTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(orchestrator.updateTask(taskId, body.get("name")));
    }
}
