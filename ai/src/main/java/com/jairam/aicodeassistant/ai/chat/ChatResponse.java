package com.jairam.aicodeassistant.ai.chat;

/**
 * A completed (blocking) chat response.
 *
 * @param content the assistant's full reply text
 * @param usage token accounting reported by the provider (may be UNKNOWN)
 * @param finishReason why generation stopped (e.g. {@code stop}, {@code length}); null if unknown
 */
public record ChatResponse(String content, TokenUsage usage, String finishReason) {}
