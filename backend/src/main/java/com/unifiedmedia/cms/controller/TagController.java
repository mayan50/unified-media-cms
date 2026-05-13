package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.entity.Tag;
import com.unifiedmedia.cms.repository.AssetRepository;
import com.unifiedmedia.cms.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;
    private final AssetRepository assetRepository;

    @GetMapping
    public ResponseEntity<List<Tag>> list() {
        return ResponseEntity.ok(tagRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Tag> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
        Tag t = Tag.builder().name(name.trim()).build();
        return ResponseEntity.ok(tagRepository.save(t));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tag> update(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Tag t = tagRepository.findById(id).orElseThrow();
        if (body.containsKey("name")) t.setName(body.get("name").trim());
        return ResponseEntity.ok(tagRepository.save(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (assetRepository.existsByTagId(id)) {
            return ResponseEntity.badRequest().build();
        }
        tagRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
