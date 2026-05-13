package com.unifiedmedia.cms.controller;

import com.unifiedmedia.cms.entity.Language;
import com.unifiedmedia.cms.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageRepository languageRepository;

    @GetMapping
    public ResponseEntity<List<Language>> list() {
        return ResponseEntity.ok(languageRepository.findAllByOrderByCodeAsc());
    }
}
