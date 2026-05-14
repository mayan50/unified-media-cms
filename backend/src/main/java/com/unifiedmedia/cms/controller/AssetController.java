package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.AssetUpdateFullRequest;
import com.unifiedmedia.cms.entity.Asset;
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
    public ResponseEntity<List<Asset>> listAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAssetDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAssetDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Asset> updateAsset(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        Asset updated = assetService.updateAsset(id, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/full")
    public ResponseEntity<Map<String, Object>> updateAssetFull(
            @PathVariable UUID id,
            @RequestBody AssetUpdateFullRequest request) {
        return ResponseEntity.ok(assetService.updateAssetFull(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable UUID id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}
