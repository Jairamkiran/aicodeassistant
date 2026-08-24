package com.jairam.aicodeassistant.conversation.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to rename a chat session.
 *
 * @param title the new, non-blank title
 */
public record RenameSessionRequest(@NotBlank @Size(max = 200) String title) {}
