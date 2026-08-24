package com.jairam.aicodeassistant.iam.adapter.rest;

import com.jairam.aicodeassistant.iam.adapter.rest.dto.ApiKeyView;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.CreateApiKeyRequest;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.CreateApiKeyResponse;
import com.jairam.aicodeassistant.iam.application.ApiKeyService;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyScope;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-key management for the authenticated user.
 *
 * <ul>
 *   <li>{@code POST /api/v1/api-keys} — create a key; the raw value is returned once.
 *   <li>{@code GET /api/v1/api-keys} — list the caller's keys (metadata only).
 *   <li>{@code DELETE /api/v1/api-keys/{id}} — revoke a key the caller owns.
 * </ul>
 *
 * <p>These routes require an authenticated user (JWT or an API key with sufficient scope); a key
 * cannot be used to mint or manage other keys unless it carries the {@code ADMIN} scope — enforced
 * by the security chain for the create path via method security in a later hardening pass; for M2,
 * any authenticated principal manages its own keys.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
class ApiKeyController {

  private final ApiKeyService apiKeyService;

  ApiKeyController(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @PostMapping
  ResponseEntity<CreateApiKeyResponse> create(
      Authentication authentication, @Valid @RequestBody CreateApiKeyRequest request) {
    UUID owner = CurrentUser.id(authentication);
    Duration ttl =
        request.expiresInDays() == null ? null : Duration.ofDays(request.expiresInDays());
    ApiKeyService.IssueResult result =
        apiKeyService.issue(owner, request.name(), request.scopes(), ttl);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreateApiKeyResponse(
                result.apiKeyId(), result.rawKey(), result.keyPrefix(), result.expiresAt()));
  }

  @GetMapping
  List<ApiKeyView> list(Authentication authentication) {
    UUID owner = CurrentUser.id(authentication);
    return apiKeyService.list(owner).stream()
        .map(
            k ->
                new ApiKeyView(
                    k.id().value(),
                    k.name(),
                    k.keyPrefix(),
                    k.scopes().stream().map(ApiKeyScope::name).collect(Collectors.toSet()),
                    k.status().name(),
                    k.createdAt(),
                    k.expiresAt(),
                    k.lastUsedAt()))
        .toList();
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
  ResponseEntity<Void> revoke(Authentication authentication, @PathVariable String id) {
    apiKeyService.revoke(CurrentUser.id(authentication), id);
    return ResponseEntity.noContent().build();
  }
}
