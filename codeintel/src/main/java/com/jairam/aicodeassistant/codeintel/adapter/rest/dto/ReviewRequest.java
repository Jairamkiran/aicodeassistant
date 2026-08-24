package com.jairam.aicodeassistant.codeintel.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to review a repository.
 *
 * @param organizationId tenant scope (required)
 * @param repositoryId repository to review (required)
 * @param focus what to review, e.g. "error handling in the payment flow" (required)
 */
public record ReviewRequest(
    @NotNull UUID organizationId, @NotNull UUID repositoryId, @NotBlank String focus) {}
