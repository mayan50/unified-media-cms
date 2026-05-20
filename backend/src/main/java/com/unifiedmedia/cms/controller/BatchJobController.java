package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.JobSubmitRequest;
import com.unifiedmedia.cms.entity.BatchJob;
import com.unifiedmedia.cms.service.BatchJobService;
import com.unifiedmedia.cms.pipeline.engine.PipelineGraphParser;
import com.unifiedmedia.cms.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobService jobService;
    private final TaskService taskService;
    private final PipelineGraphParser graphParser;

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

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        return ResponseEntity.ok(jobService.getPendingCount());
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

    @PostMapping("/validate-graph")
    public ResponseEntity<Map<String, Object>> validateGraph(@RequestBody String graphJson) {
        try {
            PipelineGraphParser.DagResult result = graphParser.parseAndValidate(graphJson);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "图结构合法，无死循环",
                    "nodeCount", result.nodes().size(), "topoSorted", result.sorted()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "无效的图结构: " + e.getMessage()));
        }
    }

    @GetMapping("/{jobId}/tasks")
    public ResponseEntity<Map<String, Object>> getJobTasks(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(taskService.getTasksPaged(jobId, page, size));
    }
}
