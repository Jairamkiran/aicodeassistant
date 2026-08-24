package com.jairam.aicodeassistant.repository.adapter.rest.dto;

import java.time.Instant;
import java.util.UUID;

/** API view of an imported repository. */
public record RepositoryView(
    UUID id,
    UUID organizationId,
    String provider,
    String owner,
    String name,
    String cloneUrl,
    String defaultBranch,
    boolean isPrivate,
    String status,
    String statusDetail,
    Instant createdAt,
    Instant updatedAt) {}
