package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.entity.*;
import com.unifiedmedia.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskNodeLogRepository taskNodeLogRepository;
    private final AssetRepository assetRepository;
    private final AssetFileRepository assetFileRepository;
    private final BookDetailRepository bookDetailRepository;
    private final AssetCreatorRepository assetCreatorRepository;
    private final CreatorRepository creatorRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final ExternalIdRepository externalIdRepository;
    private final com.unifiedmedia.cms.plugin.NodeRegistry nodeRegistry;

    // ==================== 查询 ====================

    public Optional<Task> getTask(UUID id) {
        return taskRepository.findById(id);
    }

    public List<TaskNodeLog> getTaskNodeLogs(UUID taskId) {
        return taskNodeLogRepository.findByTaskIdOrderByStartTimeAsc(taskId);
    }

    public Map<String, Object> getAllTasksPaged(int page, int size) {
        var all = taskRepository.findAllByOrderByCreatedAtDesc();
        int total = all.size();
        int from = (page - 1) * size, to = Math.min(from + size, total);
        return Map.of("items", from < total ? all.subList(from, to) : List.of(), "total", total, "page", page, "size", size);
    }

    public Map<String, Object> getTasksPaged(UUID jobId, int page, int size) {
        var all = taskRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        int total = all.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        var nodeLabels = nodeRegistry.getNodeLabels();
        return Map.of("items", from < total ? all.subList(from, to) : List.of(),
                "total", total, "page", page, "size", size, "nodeLabels", nodeLabels);
    }

    public Map<String, Object> getTaskDetail(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);

        if (task.getAssetId() != null) {
            assetRepository.findById(task.getAssetId()).ifPresent(asset -> {
                result.put("asset", asset);
                bookDetailRepository.findByAssetId(task.getAssetId()).ifPresent(bd -> result.put("bookDetail", bd));

                List<AssetCreator> acs = assetCreatorRepository.findByAssetId(task.getAssetId());
                if (!acs.isEmpty()) {
                    List<UUID> creatorIds = acs.stream().map(AssetCreator::getCreatorId).distinct().toList();
                    Map<UUID, Creator> creatorMap = creatorRepository.findAllById(creatorIds).stream()
                            .collect(Collectors.toMap(Creator::getId, c -> c));
                    List<Map<String, Object>> creators = new ArrayList<>();
                    for (AssetCreator ac : acs) {
                        Creator c = creatorMap.get(ac.getCreatorId());
                        if (c != null) {
                            Map<String, Object> cm = new LinkedHashMap<>();
                            cm.put("id", c.getId());
                            cm.put("name", c.getName());
                            cm.put("role", ac.getRole());
                            creators.add(cm);
                        }
                    }
                    if (!creators.isEmpty()) result.put("creators", creators);
                }
            });
        }

        // Tags & categories (via Asset @ManyToMany)
        if (task.getAssetId() != null) {
            Asset asset = assetRepository.findById(task.getAssetId()).orElse(null);
            if (asset != null) {
                if (!asset.getTags().isEmpty()) {
                    List<Map<String, Object>> tags = asset.getTags().stream().map(t -> {
                        Map<String, Object> tm = new LinkedHashMap<>();
                        tm.put("id", t.getId());
                        tm.put("name", t.getName());
                        return tm;
                    }).collect(Collectors.toList());
                    result.put("tags", tags);
                }
                if (!asset.getCategories().isEmpty()) {
                    List<Map<String, Object>> categories = asset.getCategories().stream().map(c -> {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("id", c.getId());
                        cm.put("name", c.getName());
                        cm.put("mediaType", c.getMediaType());
                        return cm;
                    }).collect(Collectors.toList());
                    result.put("categories", categories);
                }

                List<ExternalId> extIds = externalIdRepository.findByAssetId(task.getAssetId());
                if (!extIds.isEmpty()) {
                    result.put("externalIds", extIds.stream().map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", e.getId());
                        m.put("source", e.getSource());
                        m.put("identifier", e.getIdentifier());
                        return m;
                    }).collect(Collectors.toList()));
                }
            }
        }

        List<TaskNodeLog> logs = taskNodeLogRepository.findByTaskIdOrderByStartTimeAsc(taskId);
        result.put("nodeLogs", logs);

        return result;
    }

    // ==================== 删除 ====================

    @Transactional
    public void deleteTask(UUID taskId, boolean deleteAsset, boolean deleteSourceFiles) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        taskNodeLogRepository.deleteByTaskId(taskId);

        if (task.getAssetId() != null) {
            if (deleteAsset) {
                assetCreatorRepository.deleteByAssetId(task.getAssetId());
                bookDetailRepository.findByAssetId(task.getAssetId())
                        .ifPresent(bookDetailRepository::delete);
                assetFileRepository.deleteByAssetId(task.getAssetId());
                assetRepository.deleteById(task.getAssetId());
                log.info("[TaskService] Deleted asset {} and related data", task.getAssetId());
            }
            if (deleteSourceFiles) {
                // Source file deletion requires storage node access — defer to caller or future implementation
                log.info("[TaskService] Source file deletion requested for task {} (not yet implemented)", taskId);
            }
        }

        taskRepository.delete(task);
        log.info("[TaskService] Deleted task {}", taskId);
    }
}
