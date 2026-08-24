package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request body. Bean-validation constraints are the first line of defence; the domain
 * {@code Email} value object re-validates on the way in.
 *
 * @param email the user's email
 * @param password raw password (8–128 chars); never logged or stored in the clear
 * @param displayName human-facing name
 */
public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 100) String displayName) {}
