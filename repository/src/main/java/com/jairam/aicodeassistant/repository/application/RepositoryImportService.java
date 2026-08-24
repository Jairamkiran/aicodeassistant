package com.jairam.aicodeassistant.repository.application;

import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.integration.github.GitHubGateway;
import com.jairam.aicodeassistant.integration.github.GitHubRepo;
import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import com.jairam.aicodeassistant.platform.error.ConflictException;
import com.jairam.aicodeassistant.repository.domain.Repository;
import com.jairam.aicodeassistant.repository.domain.RepositoryImportRequested;
import com.jairam.aicodeassistant.repository.domain.RepositoryStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for browsing linkable GitHub repos and importing one into an organization.
 *
 * <p>Provider access goes through {@link GitHubGateway} (no GitHub types leak here beyond the
 * neutral {@link GitHubRepo}). Authorization goes through the iam {@link OrganizationAccess} port —
 * importing requires at least MEMBER in the target org. Registration persists the repo and
 * publishes {@link RepositoryImportRequested} (externalized to Kafka via the outbox) in one
 * transaction.
 */
@Service
public class RepositoryImportService {

  private static final Logger log = LoggerFactory.getLogger(RepositoryImportService.class);

  private final RepositoryStore repositories;
  private final GitHubGateway github;
  private final OrganizationAccess organizationAccess;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RepositoryImportService(
      RepositoryStore repositories,
      GitHubGateway github,
      OrganizationAccess organizationAccess,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repositories = repositories;
    this.github = github;
    this.organizationAccess = organizationAccess;
    this.events = events;
    this.clock = clock;
  }

  /** Lists the caller's GitHub repositories available to import. */
  public List<GitHubRepo> listImportableRepositories(UUID userId) {
    return github.listRepositories(userId);
  }

  /**
   * Imports a GitHub repository (by owner/name) into an organization.
   *
   * @throws NotAnOrgMemberException if the caller lacks access to the organization
   * @throws ConflictException if the repository is already registered in the org
   */
  @Transactional
  public ImportResult importGitHubRepository(
      UUID userId, UUID organizationId, String owner, String name) {
    if (!organizationAccess.canContribute(userId, organizationId)) {
      throw new NotAnOrgMemberException(organizationId);
    }

    GitHubRepo repo = github.getRepository(userId, owner, name);

    if (repositories.existsByOrgProviderExternalId(organizationId, "GITHUB", repo.externalId())) {
      throw new ConflictException(
          "Repository is already registered in this organization",
          java.util.Map.of("owner", owner, "name", name));
    }

    Instant now = clock.instant();
    Repository registered =
        repositories.save(
            Repository.register(
                organizationId,
                "GITHUB",
                repo.externalId(),
                repo.owner(),
                repo.name(),
                repo.cloneUrl(),
                repo.defaultBranch(),
                repo.isPrivate(),
                userId,
                now));

    // Outbox-externalized event that triggers the M4 indexing saga.
    events.publishEvent(
        RepositoryImportRequested.of(
            registered.id().value(),
            organizationId,
            registered.cloneUrl(),
            registered.defaultBranch(),
            now));
    events.publishEvent(
        AuditSignal.builder("REPOSITORY_IMPORT_REQUESTED")
            .success(true)
            .actor("USER", userId.toString())
            .target("REPOSITORY", registered.id().toString())
            .occurredAt(now)
            .build());
    log.info(
        "Registered repository {} ({}/{}) for org {} — import requested",
        registered.id(),
        owner,
        name,
        organizationId);

    return new ImportResult(registered.id().value(), registered.status().name());
  }

  /**
   * Requests a re-index of an already-imported repository: moves it back to IMPORTING and re-emits
   * the import-requested event so the worker saga runs again. Requires MEMBER+ in the org.
   *
   * @throws NotAnOrgMemberException if the caller lacks contribute access
   * @throws ResourceNotFoundException if the repository does not exist
   * @throws ConflictException if the repository is not in a terminal state
   */
  @Transactional
  public void reindex(UUID userId, String repositoryId) {
    Repository repo =
        repositories
            .findById(com.jairam.aicodeassistant.repository.domain.RepositoryId.of(repositoryId))
            .orElseThrow(
                () ->
                    new com.jairam.aicodeassistant.platform.error.ResourceNotFoundException(
                        "Repository", repositoryId));
    if (!organizationAccess.canContribute(userId, repo.organizationId())) {
      throw new NotAnOrgMemberException(repo.organizationId());
    }

    Instant now = clock.instant();
    try {
      repo.requestReindex(now);
    } catch (IllegalStateException e) {
      throw new ConflictException(e.getMessage(), java.util.Map.of("repositoryId", repositoryId));
    }
    repositories.save(repo);

    events.publishEvent(
        RepositoryImportRequested.of(
            repo.id().value(), repo.organizationId(), repo.cloneUrl(), repo.defaultBranch(), now));
    events.publishEvent(
        AuditSignal.builder("REPOSITORY_REINDEX_REQUESTED")
            .success(true)
            .actor("USER", userId.toString())
            .target("REPOSITORY", repo.id().toString())
            .occurredAt(now)
            .build());
    log.info("Re-index requested for repository {} by user {}", repo.id(), userId);
  }

  /**
   * Deletes an imported repository. Requires MEMBER+ in the org. Removing the repository row
   * cascades to its indexed chunks and index-job rows via the schema's foreign keys.
   *
   * @throws NotAnOrgMemberException if the caller lacks contribute access
   * @throws ResourceNotFoundException if the repository does not exist
   */
  @Transactional
  public void delete(UUID userId, String repositoryId) {
    var id = com.jairam.aicodeassistant.repository.domain.RepositoryId.of(repositoryId);
    Repository repo =
        repositories
            .findById(id)
            .orElseThrow(
                () ->
                    new com.jairam.aicodeassistant.platform.error.ResourceNotFoundException(
                        "Repository", repositoryId));
    if (!organizationAccess.canContribute(userId, repo.organizationId())) {
      throw new NotAnOrgMemberException(repo.organizationId());
    }

    repositories.deleteById(id);
    events.publishEvent(
        AuditSignal.builder("REPOSITORY_DELETED")
            .success(true)
            .actor("USER", userId.toString())
            .target("REPOSITORY", repositoryId)
            .occurredAt(clock.instant())
            .build());
    log.info("Deleted repository {} by user {}", repositoryId, userId);
  }

  /** Result of an import request. */
  public record ImportResult(UUID repositoryId, String status) {}

  /** Raised when the caller is not a member of the target organization. HTTP 403. */
  public static final class NotAnOrgMemberException
      extends com.jairam.aicodeassistant.platform.error.ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAnOrgMemberException(UUID organizationId) {
      super(
          com.jairam.aicodeassistant.platform.error.ErrorType.AUTHORIZATION,
          org.springframework.http.HttpStatus.FORBIDDEN,
          "You are not a member of this organization",
          java.util.Map.of("organizationId", organizationId.toString()));
    }
  }
}
