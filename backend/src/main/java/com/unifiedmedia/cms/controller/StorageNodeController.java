package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.StorageNodeRequest;
import com.unifiedmedia.cms.entity.StorageNode;
import com.unifiedmedia.cms.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/storage-nodes")
@RequiredArgsConstructor
public class StorageNodeController {

    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<List<StorageNode>> listNodes() {
        return ResponseEntity.ok(storageService.getAllNodes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StorageNode> getNode(@PathVariable UUID id) {
        return ResponseEntity.ok(storageService.getNode(id));
    }

    @PostMapping
    public ResponseEntity<StorageNode> createNode(@Valid @RequestBody StorageNodeRequest request) {
        StorageNode node = storageService.createNode(
                request.getName(), request.getProviderType(),
                request.getConnectionConfig(), request.getIsReadonly()
        );
        return ResponseEntity.ok(node);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StorageNode> updateNode(
            @PathVariable UUID id,
            @Valid @RequestBody StorageNodeRequest request) {
        StorageNode node = storageService.updateNode(
                id, request.getName(), request.getProviderType(),
                request.getConnectionConfig(), request.getIsReadonly()
        );
        return ResponseEntity.ok(node);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable UUID id) {
        storageService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/browse")
    public ResponseEntity<List<Map<String, Object>>> browse(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "/") String path) {
        StorageNode node = storageService.getNode(id);
        String absolutePath = storageService.resolveAbsolutePath(node, path);

        List<Map<String, Object>> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(absolutePath))) {
            for (Path entry : stream) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", entry.getFileName().toString());
                item.put("is_dir", Files.isDirectory(entry));
                item.put("path", path.equals("/") ? "/" + entry.getFileName() : path + "/" + entry.getFileName());
                entries.add(item);
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }

        entries.sort((a, b) -> {
            boolean aDir = (boolean) a.get("is_dir");
            boolean bDir = (boolean) b.get("is_dir");
            if (aDir != bDir) return aDir ? -1 : 1;
            return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
        });

        return ResponseEntity.ok(entries);
    }
}
