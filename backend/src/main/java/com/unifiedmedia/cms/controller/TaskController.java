package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.entity.Task;
import com.unifiedmedia.cms.entity.TaskNodeLog;
import com.unifiedmedia.cms.service.BatchJobService;
import com.unifiedmedia.cms.service.TaskService;
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

    private final TaskService taskService;
    private final BatchJobService jobService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAllTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(taskService.getAllTasksPaged(page, size, search, status));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTask(@PathVariable UUID taskId) {
        return taskService.getTask(taskId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<TaskNodeLog>> getTaskNodeLogs(@PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskNodeLogs(taskId));
    }

    @GetMapping("/{taskId}/detail")
    public ResponseEntity<Map<String, Object>> getTaskDetail(@PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId,
            @RequestParam(required = false, defaultValue = "false") boolean deleteAsset,
            @RequestParam(required = false, defaultValue = "false") boolean deleteSourceFiles) {
        taskService.deleteTask(taskId, deleteAsset, deleteSourceFiles);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/retry")
    public ResponseEntity<Map<String, Object>> retryTask(@PathVariable UUID taskId) {
        jobService.retryTask(taskId);
        return ResponseEntity.ok(Map.of("taskId", taskId, "status", "RETRYING"));
    }
}
