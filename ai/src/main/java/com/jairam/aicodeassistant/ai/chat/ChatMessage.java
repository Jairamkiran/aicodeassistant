package com.jairam.aicodeassistant.ai.chat;

import java.util.Objects;

/**
 * One message in a chat conversation, in provider-neutral form. Provider JSON shapes are mapped
 * from/to this inside the adapters.
 *
 * @param role who authored the message
 * @param content the message text
 */
public record ChatMessage(Role role, String content) {

  public ChatMessage {
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(content, "content");
  }

  public static ChatMessage system(String content) {
    return new ChatMessage(Role.SYSTEM, content);
  }

  public static ChatMessage user(String content) {
    return new ChatMessage(Role.USER, content);
  }

  public static ChatMessage assistant(String content) {
    return new ChatMessage(Role.ASSISTANT, content);
  }

  /** Message author role, mapped to each provider's own role vocabulary. */
  public enum Role {
    SYSTEM,
    USER,
    ASSISTANT
  }
}
