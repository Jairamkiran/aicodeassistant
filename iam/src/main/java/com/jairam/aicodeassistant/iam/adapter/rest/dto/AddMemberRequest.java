package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import com.jairam.aicodeassistant.iam.domain.model.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request to add (or re-role) a member in an organization.
 *
 * @param userId the user to add
 * @param role the role to grant
 */
public record AddMemberRequest(@NotNull java.util.UUID userId, @NotNull Role role) {}
