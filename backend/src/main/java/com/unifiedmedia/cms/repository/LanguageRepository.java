package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LanguageRepository extends JpaRepository<Language, UUID> {
    Optional<Language> findByCode(String code);
    List<Language> findAllByOrderByCodeAsc();
}
