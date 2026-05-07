package com.apiguard.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 6) String password
) {}
