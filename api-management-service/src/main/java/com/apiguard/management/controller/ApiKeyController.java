package com.apiguard.management.controller;

import com.apiguard.management.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService service;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateKey(
            @RequestParam UUID apiId,
            @RequestParam(defaultValue = "FREE") String plan) {
        String rawKey = service.createApiKey(apiId, plan);
        return ResponseEntity.ok(Map.of(
            "apiKey", rawKey,
            "message", "Keep this key safe! It will not be shown again."
        ));
    }
}
