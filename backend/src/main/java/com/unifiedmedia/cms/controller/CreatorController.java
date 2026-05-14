package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.entity.Creator;
import com.unifiedmedia.cms.repository.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorRepository creatorRepository;

    @GetMapping
    public ResponseEntity<List<Creator>> list(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(creatorRepository.findAll().stream()
                    .filter(c -> c.getName().contains(search)).toList());
        }
        return ResponseEntity.ok(creatorRepository.findAll());
    }
}
