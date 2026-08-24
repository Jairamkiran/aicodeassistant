package com.jairam.aicodeassistant.conversation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One message in a session. User turns carry no citations; assistant turns carry the citations that
 * grounded the answer.
 *
 * @param seq monotonically increasing position within the session (0-based)
 * @param role who authored the turn
 * @param content the message text
 * @param citations sources for an assistant turn (empty for user turns)
 * @param createdAt when the turn was recorded
 */
public record ChatTurn(
    int seq, Role role, String content, List<Citation> citations, Instant createdAt) {

  public ChatTurn {
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(content, "content");
    citations = citations == null ? List.of() : List.copyOf(citations);
  }

  public static ChatTurn user(int seq, String content, Instant now) {
    return new ChatTurn(seq, Role.USER, content, List.of(), now);
  }

  public static ChatTurn assistant(int seq, String content, List<Citation> citations, Instant now) {
    return new ChatTurn(seq, Role.ASSISTANT, content, citations, now);
  }

  /** Turn author. */
  public enum Role {
    USER,
    ASSISTANT
  }
}
