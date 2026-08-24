package com.jairam.aicodeassistant.repository.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void registerStartsInRegisteredStatus() {
    Repository repo =
        Repository.register(
            UUID.randomUUID(),
            "GITHUB",
            "42",
            "octo",
            "acme",
            "https://github.com/octo/acme.git",
            "main",
            false,
            UUID.randomUUID(),
            NOW);

    assertThat(repo.status()).isEqualTo(ImportStatus.REGISTERED);
    assertThat(repo.owner()).isEqualTo("octo");
    assertThat(repo.provider()).isEqualTo("GITHUB");
  }

  @Test
  void registerRejectsBlankRequiredFields() {
    assertThatThrownBy(
            () ->
                Repository.register(
                    UUID.randomUUID(),
                    "GITHUB",
                    "42",
                    "",
                    "acme",
                    "url",
                    "main",
                    false,
                    UUID.randomUUID(),
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void importRequestedEventCarriesCloneDetails() {
    UUID repoId = UUID.randomUUID();
    UUID orgId = UUID.randomUUID();
    var event =
        RepositoryImportRequested.of(
            repoId, orgId, "https://github.com/octo/acme.git", "main", NOW);

    assertThat(event.repositoryId()).isEqualTo(repoId);
    assertThat(event.organizationId()).isEqualTo(orgId);
    assertThat(event.cloneUrl()).contains("acme");
    assertThat(event.defaultBranch()).isEqualTo("main");
    assertThat(event.eventId()).isNotNull();
  }
}
