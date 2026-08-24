package com.jairam.aicodeassistant.conversation.adapter.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to start a chat session.
 *
 * @param organizationId tenant scope (required; caller must be able to read it)
 * @param repositoryId optional single-repo scope (null = org-wide chat)
 * @param title optional session title (defaulted if blank)
 */
public record CreateSessionRequest(
    @NotNull UUID organizationId, UUID repositoryId, @Size(max = 200) String title) {}
