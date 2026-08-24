package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import java.util.UUID;

/**
 * Response after creating an organization.
 *
 * @param organizationId the new organization id
 * @param slug the derived URL-safe slug
 */
public record CreateOrganizationResponse(UUID organizationId, String slug) {}
