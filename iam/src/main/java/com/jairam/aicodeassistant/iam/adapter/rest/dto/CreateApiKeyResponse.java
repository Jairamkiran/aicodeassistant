package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response to API-key creation. Contains the raw key, which is returned <em>only here, only
 * once</em> — it is never retrievable again.
 *
 * @param id the key's id (for later revocation)
 * @param apiKey the full raw key ({@code aca_<prefix>.<secret>}) — store it now
 * @param keyPrefix the non-secret prefix (shown in listings)
 * @param expiresAt expiry instant, or null if the key never expires
 */
public record CreateApiKeyResponse(UUID id, String apiKey, String keyPrefix, Instant expiresAt) {}
