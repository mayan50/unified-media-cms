package com.unifiedmedia.cms.service;

import com.unifiedmedia.cms.pipeline.core.StorageAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 存储适配器注册表 — 自动发现所有 StorageAdapter 实现。
 */
@Component
@RequiredArgsConstructor
public class StorageAdapterRegistry {

    private final List<StorageAdapter> adapters;

    public StorageAdapter getAdapter(String providerType) {
        return adapters.stream()
                .filter(a -> a.supports().equalsIgnoreCase(providerType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("No storage adapter for: " + providerType));
    }
}
