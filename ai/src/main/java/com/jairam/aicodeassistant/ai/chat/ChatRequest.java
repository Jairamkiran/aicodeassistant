package com.jairam.aicodeassistant.ai.chat;

import java.util.List;
import java.util.Objects;

/**
 * A chat completion request. {@code model} is optional — null means "use the provider's configured
 * default" — so callers need not know provider model names.
 *
 * @param messages the conversation so far (system/user/assistant), non-empty
 * @param model optional model override (null = provider default)
 * @param temperature optional sampling temperature (null = provider default)
 * @param maxTokens optional max completion tokens (null = provider default)
 */
public record ChatRequest(
    List<ChatMessage> messages, String model, Double temperature, Integer maxTokens) {

  public ChatRequest {
    Objects.requireNonNull(messages, "messages");
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("messages must not be empty");
    }
    messages = List.copyOf(messages);
  }

  /** Convenience: a request from just a message list, all provider defaults. */
  public static ChatRequest of(List<ChatMessage> messages) {
    return new ChatRequest(messages, null, null, null);
  }
}
