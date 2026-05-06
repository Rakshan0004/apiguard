package com.apiguard.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

public record ApiRegistrationRequest(
    @NotBlank String name,
    @NotBlank @URL String targetUrl,
    @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String proxyPath
) {}
