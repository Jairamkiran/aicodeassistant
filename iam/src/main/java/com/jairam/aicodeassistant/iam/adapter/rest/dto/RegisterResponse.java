package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import java.util.UUID;

/**
 * Response returned after successful registration.
 *
 * @param userId the new user's id
 */
public record RegisterResponse(UUID userId) {}
