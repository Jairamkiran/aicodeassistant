package com.jairam.aicodeassistant.conversation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Chat session aggregate — a conversation scoped to an organization (and optionally one
 * repository), owned by a user. Holds its turns in order.
 *
 * <p>Memory windowing ({@link #recentTurns(int)}) is a domain concern: the RAG orchestrator
 * includes only the last N turns in the prompt (see ADR-0013), keeping context bounded without an
 * extra summarization call.
 */
public final class ChatSession {

  private final UUID id;
  private final UUID organizationId;
  private final UUID repositoryId; // nullable: org-wide chat
  private final UUID userId;
  private String title;
  private final List<ChatTurn> turns;
  private final Instant createdAt;
  private Instant updatedAt;
  private final long version;

  private ChatSession(
      UUID id,
      UUID organizationId,
      UUID repositoryId,
      UUID userId,
      String title,
      List<ChatTurn> turns,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.repositoryId = repositoryId;
    this.userId = Objects.requireNonNull(userId, "userId");
    this.title = requireText(title, "title");
    this.turns = new ArrayList<>(turns == null ? List.of() : turns);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /** Starts a new session. */
  public static ChatSession start(
      UUID organizationId, UUID repositoryId, UUID userId, String title, Instant now) {
    return new ChatSession(
        UUID.randomUUID(),
        organizationId,
        repositoryId,
        userId,
        title,
        new ArrayList<>(),
        now,
        now,
        0L);
  }

  /** Reconstructs from persistence with its turns. */
  public static ChatSession rehydrate(
      UUID id,
      UUID organizationId,
      UUID repositoryId,
      UUID userId,
      String title,
      List<ChatTurn> turns,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new ChatSession(
        id, organizationId, repositoryId, userId, title, turns, createdAt, updatedAt, version);
  }

  /** Appends a turn, assigning the next sequence number. Returns the created turn. */
  public ChatTurn addUserTurn(String content, Instant now) {
    ChatTurn turn = ChatTurn.user(turns.size(), content, now);
    turns.add(turn);
    this.updatedAt = now;
    return turn;
  }

  /** Appends an assistant turn with citations. */
  public ChatTurn addAssistantTurn(String content, List<Citation> citations, Instant now) {
    ChatTurn turn = ChatTurn.assistant(turns.size(), content, citations, now);
    turns.add(turn);
    this.updatedAt = now;
    return turn;
  }

  /** Renames the session. */
  public void rename(String newTitle, Instant now) {
    this.title = requireText(newTitle, "title");
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  /** The last {@code limit} turns (oldest first), for windowed memory. */
  public List<ChatTurn> recentTurns(int limit) {
    if (limit <= 0 || turns.isEmpty()) {
      return List.of();
    }
    int from = Math.max(0, turns.size() - limit);
    return List.copyOf(turns.subList(from, turns.size()));
  }

  public UUID id() {
    return id;
  }

  public UUID organizationId() {
    return organizationId;
  }

  public UUID repositoryId() {
    return repositoryId;
  }

  public UUID userId() {
    return userId;
  }

  public String title() {
    return title;
  }

  public List<ChatTurn> turns() {
    return List.copyOf(turns);
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
