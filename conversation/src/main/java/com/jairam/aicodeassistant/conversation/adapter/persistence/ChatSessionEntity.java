package com.jairam.aicodeassistant.conversation.adapter.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JPA entity for {@code chat_sessions} with its ordered turns. */
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "repository_id")
  private UUID repositoryId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false)
  private String title;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "session",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("seq ASC")
  private List<ChatTurnEntity> turns = new ArrayList<>();

  protected ChatSessionEntity() {
    // Required by JPA.
  }

  ChatSessionEntity(
      UUID id,
      UUID organizationId,
      UUID repositoryId,
      UUID userId,
      String title,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.organizationId = organizationId;
    this.repositoryId = repositoryId;
    this.userId = userId;
    this.title = title;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  void replaceTurns(List<ChatTurnEntity> newTurns) {
    this.turns.clear();
    for (ChatTurnEntity t : newTurns) {
      t.setSession(this);
      this.turns.add(t);
    }
  }

  UUID getId() {
    return id;
  }

  UUID getOrganizationId() {
    return organizationId;
  }

  UUID getRepositoryId() {
    return repositoryId;
  }

  UUID getUserId() {
    return userId;
  }

  String getTitle() {
    return title;
  }

  void setTitle(String title) {
    this.title = title;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  long getVersion() {
    return version;
  }

  List<ChatTurnEntity> getTurns() {
    return turns;
  }
}
