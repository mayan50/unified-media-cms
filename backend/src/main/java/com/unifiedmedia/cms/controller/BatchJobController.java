package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.JobSubmitRequest;
import com.unifiedmedia.cms.entity.BatchJob;
import com.unifiedmedia.cms.entity.Task;
import com.unifiedmedia.cms.entity.TaskNodeLog;
import com.unifiedmedia.cms.service.BatchJobService;
import com.unifiedmedia.cms.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobService jobService;
    private final TaskService taskService;

    // ==================== Job CRUD ====================

    @PostMapping
    public ResponseEntity<BatchJob> createJob(@RequestBody JobSubmitRequest req) {
        return ResponseEntity.ok(jobService.submitJob(req.getName(), req.getTemplateId(), req.getInputPath(), req.getOutputPath()));
    }

    @PostMapping("/{jobId}/start")
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable UUID jobId) {
        jobService.startJob(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", "RUNNING"));
    }

    @PostMapping("/{jobId}/continue")
    public ResponseEntity<Map<String, Object>> continueJob(@PathVariable UUID jobId) {
        jobService.continueJob(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", "RUNNING"));
    }

    @PostMapping("/{jobId}/stop")
    public ResponseEntity<Map<String, Object>> stopJob(@PathVariable UUID jobId) {
        jobService.stopJob(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", "FAILED"));
    }

    @PostMapping("/{jobId}/reset")
    public ResponseEntity<Map<String, Object>> resetJob(@PathVariable UUID jobId) {
        jobService.resetJob(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId, "status", "PENDING"));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<BatchJob> updateJob(@PathVariable UUID jobId, @RequestBody Map<String, Object> body) {
        jobService.updateJob(jobId, body);
        return jobService.getJob(jobId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID jobId,
            @RequestParam(required = false, defaultValue = "false") boolean deleteAssets,
            @RequestParam(required = false, defaultValue = "false") boolean deleteSourceFiles) {
        jobService.deleteJob(jobId, deleteAssets, deleteSourceFiles);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(jobService.getJobsPaged(page, size, search, status, sort));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<BatchJob> getJob(@PathVariable UUID jobId) {
        return jobService.getJob(jobId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/tasks")
    public ResponseEntity<Map<String, Object>> getJobTasks(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(taskService.getTasksPaged(jobId, page, size));
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<Map<String, Object>>> getAvailableNodes() {
        return ResponseEntity.ok(jobService.getAvailableNodes().stream()
                .map(n -> Map.of("name", n.getNodeName(), "className", (Object) n.getClass().getSimpleName()))
                .collect(Collectors.toList()));
    }

    // ==================== Task CRUD (delegated to TaskService) ====================

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> listAllTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(taskService.getAllTasksPaged(page, size));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Task> getTask(@PathVariable UUID taskId) {
        return taskService.getTask(taskId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tasks/{taskId}/logs")
    public ResponseEntity<List<TaskNodeLog>> getTaskNodeLogs(@PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskNodeLogs(taskId));
    }

    @GetMapping("/tasks/{taskId}/detail")
    public ResponseEntity<Map<String, Object>> getTaskDetail(@PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId,
            @RequestParam(required = false, defaultValue = "false") boolean deleteAsset,
            @RequestParam(required = false, defaultValue = "false") boolean deleteSourceFiles) {
        taskService.deleteTask(taskId, deleteAsset, deleteSourceFiles);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ResponseEntity<Map<String, Object>> retryTask(@PathVariable UUID taskId) {
        jobService.retryTask(taskId);
        return ResponseEntity.ok(Map.of("taskId", taskId, "status", "RETRYING"));
    }
}
