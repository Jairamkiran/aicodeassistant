package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Request to create an API key.
 *
 * @param name human-facing label for the key
 * @param scopes permissions the key carries (must be non-empty)
 * @param expiresInDays optional expiry in days; null/absent = never expires
 */
public record CreateApiKeyRequest(
    @NotBlank @Size(max = 100) String name,
    @NotEmpty Set<ApiKeyScope> scopes,
    Long expiresInDays) {}
