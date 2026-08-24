package com.jairam.aicodeassistant.conversation.adapter.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** API view of a chat session (optionally with its turns). */
public record SessionView(
    UUID id,
    UUID organizationId,
    UUID repositoryId,
    String title,
    Instant createdAt,
    Instant updatedAt,
    List<TurnView> turns) {

  /** A turn in the session; assistant turns include citations. */
  public record TurnView(int seq, String role, String content, List<CitationView> citations) {}

  /** A citation with file:line provenance. */
  public record CitationView(
      int index, UUID chunkId, UUID repositoryId, String filePath, int startLine, int endLine) {}
}
