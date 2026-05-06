package com.apiguard.management.controller;

import com.apiguard.management.dto.ApiRegistrationRequest;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.service.ApiRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
public class ApiRegistrationController {

    private final ApiRegistrationService service;

    @PostMapping
    public ResponseEntity<RegisteredApi> registerApi(@Valid @RequestBody ApiRegistrationRequest request) {
        // In a real app, email would come from SecurityContext
        String ownerEmail = "demo@example.com"; 
        return ResponseEntity.ok(service.registerApi(ownerEmail, request));
    }

    @GetMapping
    public ResponseEntity<List<RegisteredApi>> listApis() {
        String ownerEmail = "demo@example.com";
        return ResponseEntity.ok(service.getUserApis(ownerEmail));
    }
}
