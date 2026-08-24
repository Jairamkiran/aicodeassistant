package com.jairam.aicodeassistant.repository.adapter.rest;

import com.jairam.aicodeassistant.repository.adapter.rest.dto.GitHubRepoView;
import com.jairam.aicodeassistant.repository.adapter.rest.dto.ImportRepositoryRequest;
import com.jairam.aicodeassistant.repository.adapter.rest.dto.RepositoryView;
import com.jairam.aicodeassistant.repository.application.RepositoryImportService;
import com.jairam.aicodeassistant.repository.application.RepositoryQueryService;
import com.jairam.aicodeassistant.repository.domain.Repository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Repository endpoints.
 *
 * <ul>
 *   <li>{@code GET /api/v1/repositories/github} — list the caller's linkable GitHub repos (via the
 *       GitHub gateway).
 *   <li>{@code POST /api/v1/repositories/import} — import one into an org (member+), registering it
 *       and requesting indexing.
 *   <li>{@code GET /api/v1/repositories?organizationId=} — list imported repos.
 *   <li>{@code GET /api/v1/repositories/{id}} — fetch one imported repo.
 * </ul>
 *
 * <p>All routes require authentication; org-scoped access is enforced in the application services
 * via the iam {@code OrganizationAccess} port.
 */
@RestController
@RequestMapping("/api/v1/repositories")
class RepositoryController {

  private final RepositoryImportService importService;
  private final RepositoryQueryService queryService;

  RepositoryController(RepositoryImportService importService, RepositoryQueryService queryService) {
    this.importService = importService;
    this.queryService = queryService;
  }

  @GetMapping("/github")
  List<GitHubRepoView> listGitHubRepositories(Authentication authentication) {
    UUID userId = currentUserId(authentication);
    return importService.listImportableRepositories(userId).stream()
        .map(
            r ->
                new GitHubRepoView(
                    r.externalId(),
                    r.owner(),
                    r.name(),
                    r.fullName(),
                    r.defaultBranch(),
                    r.isPrivate()))
        .toList();
  }

  @PostMapping("/import")
  ResponseEntity<RepositoryView> importRepository(
      Authentication authentication, @Valid @RequestBody ImportRepositoryRequest request) {
    UUID userId = currentUserId(authentication);
    var result =
        importService.importGitHubRepository(
            userId, request.organizationId(), request.owner(), request.name());
    Repository repo = queryService.getForUser(userId, result.repositoryId().toString());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(toView(repo));
  }

  @GetMapping
  List<RepositoryView> list(
      Authentication authentication, @RequestParam("organizationId") UUID organizationId) {
    UUID userId = currentUserId(authentication);
    return queryService.listInOrganization(userId, organizationId).stream()
        .map(RepositoryController::toView)
        .toList();
  }

  @GetMapping("/{id}")
  RepositoryView get(Authentication authentication, @PathVariable String id) {
    return toView(queryService.getForUser(currentUserId(authentication), id));
  }

  @PostMapping("/{id}/reindex")
  @ResponseStatus(HttpStatus.ACCEPTED)
  RepositoryView reindex(Authentication authentication, @PathVariable String id) {
    UUID userId = currentUserId(authentication);
    importService.reindex(userId, id);
    return toView(queryService.getForUser(userId, id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(Authentication authentication, @PathVariable String id) {
    importService.delete(currentUserId(authentication), id);
  }

  private static RepositoryView toView(Repository r) {
    return new RepositoryView(
        r.id().value(),
        r.organizationId(),
        r.provider(),
        r.owner(),
        r.name(),
        r.cloneUrl(),
        r.defaultBranch(),
        r.isPrivate(),
        r.status().name(),
        r.statusDetail(),
        r.createdAt(),
        r.updatedAt());
  }

  private static UUID currentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new com.jairam.aicodeassistant.platform.error.ValidationException("Not authenticated");
    }
    return UUID.fromString(authentication.getName());
  }
}
