package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create an organization. */
public record CreateOrganizationRequest(@NotBlank @Size(max = 120) String name) {}
