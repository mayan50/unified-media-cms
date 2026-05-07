package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.AssetUpdateRequest;
import com.unifiedmedia.cms.entity.MediaAsset;
import com.unifiedmedia.cms.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<List<MediaAsset>> listAssets(
            @RequestParam(required = false) String status) {
        if ("COMPLETED".equals(status)) {
            return ResponseEntity.ok(assetService.getCompletedAssets());
        }
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAssetDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAssetDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MediaAsset> updateAsset(
            @PathVariable UUID id,
            @RequestBody AssetUpdateRequest request) {
        MediaAsset updated = assetService.updateAsset(
                id, request.getTitle(), request.getSummary(),
                request.getPublishYear(), request.getCoverUrl()
        );
        return ResponseEntity.ok(updated);
    }
}
