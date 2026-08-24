package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request body. Deliberately does not use {@code @Email} — a malformed email should yield the
 * same generic "invalid credentials" response as a wrong password, not a distinct validation error
 * (avoids user enumeration).
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
