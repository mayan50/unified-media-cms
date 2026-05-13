package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.entity.Category;
import com.unifiedmedia.cms.repository.AssetRepository;
import com.unifiedmedia.cms.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    @GetMapping
    public ResponseEntity<List<Category>> list() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String mediaType = body.get("mediaType");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().build();
        Category c = Category.builder().name(name.trim()).mediaType(mediaType).build();
        return ResponseEntity.ok(categoryRepository.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Category c = categoryRepository.findById(id).orElseThrow();
        if (body.containsKey("name")) c.setName(body.get("name").trim());
        if (body.containsKey("mediaType")) c.setMediaType(body.get("mediaType"));
        return ResponseEntity.ok(categoryRepository.save(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (assetRepository.existsByCategoryId(id)) {
            return ResponseEntity.badRequest().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
