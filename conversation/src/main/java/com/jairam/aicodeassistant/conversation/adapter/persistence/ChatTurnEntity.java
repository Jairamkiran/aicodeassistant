package com.jairam.aicodeassistant.conversation.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** JPA entity for {@code chat_turns}. Citations are stored as a JSON string. */
@Entity
@Table(name = "chat_turns")
class ChatTurnEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "session_id", nullable = false)
  private ChatSessionEntity session;

  @Column(nullable = false)
  private int seq;

  @Column(nullable = false)
  private String role;

  @Column(nullable = false)
  private String content;

  @Column(nullable = false)
  @Convert(converter = CitationsJsonConverter.class)
  private List<CitationRow> citations;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ChatTurnEntity() {
    // Required by JPA.
  }

  ChatTurnEntity(
      UUID id,
      int seq,
      String role,
      String content,
      List<CitationRow> citations,
      Instant createdAt) {
    this.id = id;
    this.seq = seq;
    this.role = role;
    this.content = content;
    this.citations = citations;
    this.createdAt = createdAt;
  }

  void setSession(ChatSessionEntity session) {
    this.session = session;
  }

  UUID getId() {
    return id;
  }

  int getSeq() {
    return seq;
  }

  String getRole() {
    return role;
  }

  String getContent() {
    return content;
  }

  List<CitationRow> getCitations() {
    return citations;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  /** Persisted citation shape (decoupled from the domain record for storage). */
  record CitationRow(
      int index, UUID chunkId, UUID repositoryId, String filePath, int startLine, int endLine) {}
}
