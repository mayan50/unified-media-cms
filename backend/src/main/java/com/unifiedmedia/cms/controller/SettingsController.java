package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.dto.LlmConfigRequest;
import com.unifiedmedia.cms.entity.AppSetting;
import com.unifiedmedia.cms.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/llm")
    public ResponseEntity<Map<String, Object>> getLlmConfig() {
        return ResponseEntity.ok(settingsService.getLlmConfig());
    }

    @PostMapping("/llm")
    public ResponseEntity<AppSetting> saveLlmConfig(@Valid @RequestBody LlmConfigRequest request) {
        return ResponseEntity.ok(settingsService.saveLlmConfig(
                request.getProvider(), request.getBaseUrl(),
                request.getApiKey(), request.getModelName()
        ));
    }

    @GetMapping("/{key}")
    public ResponseEntity<AppSetting> getSetting(@PathVariable String key) {
        return settingsService.getSetting(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{key}")
    public ResponseEntity<AppSetting> saveSetting(
            @PathVariable String key,
            @RequestBody Object value) {
        return ResponseEntity.ok(settingsService.saveSetting(key, value));
    }
}
