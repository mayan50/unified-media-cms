package com.unifiedmedia.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifiedmedia.cms.entity.StorageNode;
import com.unifiedmedia.cms.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageNodeRepository storageNodeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<StorageNode> getAllNodes() {
        return storageNodeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public StorageNode getNode(UUID id) {
        return storageNodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage node not found: " + id));
    }

    @Transactional
    public StorageNode createNode(String name, String providerType, Map<String, Object> connectionConfig, Boolean isReadonly) {
        StorageNode node = StorageNode.builder()
                .name(name)
                .providerType(providerType)
                .connectionConfig(serializeJson(connectionConfig))
                .isReadonly(isReadonly != null ? isReadonly : false)
                .build();
        return storageNodeRepository.save(node);
    }

    @Transactional
    public StorageNode updateNode(UUID id, String name, String providerType, Map<String, Object> connectionConfig, Boolean isReadonly) {
        StorageNode node = storageNodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage node not found: " + id));
        if (name != null) node.setName(name);
        if (providerType != null) node.setProviderType(providerType);
        if (connectionConfig != null) node.setConnectionConfig(serializeJson(connectionConfig));
        if (isReadonly != null) node.setIsReadonly(isReadonly);
        return storageNodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(UUID id) {
        storageNodeRepository.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    public String resolveAbsolutePath(StorageNode node, String relativePath) {
        if (!"LOCAL".equals(node.getProviderType())) {
            throw new UnsupportedOperationException("Only LOCAL storage is supported in V1 for absolute path resolution");
        }
        try {
            Map<String, Object> config = objectMapper.readValue(node.getConnectionConfig(), Map.class);
            String basePath = (String) config.get("basePath");
            return basePath + "/" + relativePath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse storage node config", e);
        }
    }

    private String serializeJson(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { return "{}"; }
    }
}
