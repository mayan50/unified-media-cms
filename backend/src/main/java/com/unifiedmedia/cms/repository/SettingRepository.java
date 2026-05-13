package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SettingRepository extends JpaRepository<Setting, UUID> {
    Optional<Setting> findBySettingKey(String settingKey);
}
