package com.jairam.aicodeassistant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.integration.github.GitHubGateway;
import com.jairam.aicodeassistant.integration.github.GitHubRepo;
import com.jairam.aicodeassistant.repository.application.RepositoryImportService;
import com.jairam.aicodeassistant.repository.domain.ImportStatus;
import com.jairam.aicodeassistant.repository.domain.RepositoryImportRequested;
import com.jairam.aicodeassistant.repository.domain.RepositoryStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end repository import on H2 (no Docker): with GitHub and org-access collaborators stubbed,
 * importing a repo persists it as {@code REGISTERED} and publishes {@link
 * RepositoryImportRequested} (the trigger the M4 saga consumes). Also verifies the authorization
 * gate and duplicate-import conflict.
 */
@SpringBootTest
@ActiveProfiles("repotest")
@Import(RepositoryImportFlowTest.CapturingListener.class)
class RepositoryImportFlowTest {

  @MockitoBean private GitHubGateway gitHubGateway;
  @MockitoBean private OrganizationAccess organizationAccess;

  @Autowired private RepositoryImportService importService;
  @Autowired private RepositoryStore repositories;
  @Autowired private CapturingListener listener;

  /** Captures the externalized domain event in-process (Kafka relay is not active here). */
  static class CapturingListener {
    volatile RepositoryImportRequested last;

    @EventListener
    void on(RepositoryImportRequested event) {
      this.last = event;
    }
  }

  private static GitHubRepo sampleRepo() {
    return new GitHubRepo(
        "99", "octo", "acme", "octo/acme", "https://github.com/octo/acme.git", "main", false);
  }

  @Test
  void importPersistsRegisteredAndPublishesEvent() {
    UUID user = UUID.randomUUID();
    UUID org = UUID.randomUUID();
    when(organizationAccess.canContribute(user, org)).thenReturn(true);
    when(gitHubGateway.getRepository(user, "octo", "acme")).thenReturn(sampleRepo());

    var result = importService.importGitHubRepository(user, org, "octo", "acme");

    assertThat(result.status()).isEqualTo(ImportStatus.REGISTERED.name());
    var stored = repositories.findByOrganization(org);
    assertThat(stored).hasSize(1);
    assertThat(stored.get(0).externalId()).isEqualTo("99");
    assertThat(listener.last).isNotNull();
    assertThat(listener.last.repositoryId()).isEqualTo(result.repositoryId());
    assertThat(listener.last.cloneUrl()).contains("acme");
  }

  @Test
  void importIsRejectedForNonMember() {
    UUID user = UUID.randomUUID();
    UUID org = UUID.randomUUID();
    when(organizationAccess.canContribute(user, org)).thenReturn(false);

    assertThatThrownBy(() -> importService.importGitHubRepository(user, org, "octo", "acme"))
        .isInstanceOf(RepositoryImportService.NotAnOrgMemberException.class);
  }

  @Test
  void duplicateImportIsConflict() {
    UUID user = UUID.randomUUID();
    UUID org = UUID.randomUUID();
    when(organizationAccess.canContribute(user, org)).thenReturn(true);
    when(gitHubGateway.getRepository(user, "octo", "acme")).thenReturn(sampleRepo());

    importService.importGitHubRepository(user, org, "octo", "acme");
    assertThatThrownBy(() -> importService.importGitHubRepository(user, org, "octo", "acme"))
        .isInstanceOf(com.jairam.aicodeassistant.platform.error.ConflictException.class);
  }

  @Test
  void listImportableDelegatesToGateway() {
    UUID user = UUID.randomUUID();
    when(gitHubGateway.listRepositories(user)).thenReturn(List.of(sampleRepo()));
    assertThat(importService.listImportableRepositories(user)).hasSize(1);
  }
}
