package com.jairam.aicodeassistant.conversation.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A user's question in a session.
 *
 * @param question the natural-language question
 */
public record AskRequest(@NotBlank @Size(max = 4000) String question) {}
