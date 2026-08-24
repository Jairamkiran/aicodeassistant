package com.jairam.aicodeassistant.repository.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to import a GitHub repository into an organization.
 *
 * @param organizationId target organization (caller must be a member)
 * @param owner GitHub repo owner login
 * @param name GitHub repo name
 */
public record ImportRepositoryRequest(
    @NotNull UUID organizationId, @NotBlank String owner, @NotBlank String name) {}
