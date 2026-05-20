package com.unifiedmedia.cms.repository;

import com.unifiedmedia.cms.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
    List<Tag> findByNameIn(Collection<String> names);
}
