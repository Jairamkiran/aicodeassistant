package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Metadata view of an API key for listings. Never includes the secret or its hash — only
 * non-sensitive fields.
 */
public record ApiKeyView(
    UUID id,
    String name,
    String keyPrefix,
    Set<String> scopes,
    String status,
    Instant createdAt,
    Instant expiresAt,
    Instant lastUsedAt) {}
