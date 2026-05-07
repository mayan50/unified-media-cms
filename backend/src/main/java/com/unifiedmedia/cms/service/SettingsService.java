package com.unifiedmedia.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.entity.AppSetting;
import com.unifiedmedia.cms.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<AppSetting> getSetting(String key) {
        return appSettingRepository.findBySettingKey(key);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLlmConfig() {
        return appSettingRepository.findBySettingKey("llm_config")
                .map(s -> {
                    try { return objectMapper.readValue(s.getSettingValue(), Map.class); }
                    catch (Exception e) { return Map.of(); }
                })
                .orElse(Map.of());
    }

    @Transactional
    public AppSetting saveSetting(String key, Object value) {
        AppSetting setting = appSettingRepository.findBySettingKey(key)
                .orElse(AppSetting.builder().settingKey(key).build());
        try {
            setting.setSettingValue(value instanceof String s ? s : objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            setting.setSettingValue("{}");
        }
        return appSettingRepository.save(setting);
    }

    @Transactional
    public AppSetting saveLlmConfig(String provider, String baseUrl, String apiKey, String modelName) {
        Map<String, Object> config = Map.of(
                "provider", provider != null ? provider : "ollama",
                "baseUrl", baseUrl != null ? baseUrl : "",
                "apiKey", apiKey != null ? apiKey : "",
                "modelName", modelName != null ? modelName : ""
        );
        return saveSetting("llm_config", config);
    }
}
