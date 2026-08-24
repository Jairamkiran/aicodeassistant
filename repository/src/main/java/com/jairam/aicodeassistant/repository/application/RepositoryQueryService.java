package com.jairam.aicodeassistant.repository.application;

import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import com.jairam.aicodeassistant.repository.domain.Repository;
import com.jairam.aicodeassistant.repository.domain.RepositoryId;
import com.jairam.aicodeassistant.repository.domain.RepositoryStore;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use cases for imported repositories, authorized by org membership. */
@Service
public class RepositoryQueryService {

  private final RepositoryStore repositories;
  private final OrganizationAccess organizationAccess;

  public RepositoryQueryService(
      RepositoryStore repositories, OrganizationAccess organizationAccess) {
    this.repositories = repositories;
    this.organizationAccess = organizationAccess;
  }

  /** Lists repositories in an organization the caller belongs to. */
  @Transactional(readOnly = true)
  public List<Repository> listInOrganization(UUID userId, UUID organizationId) {
    if (!organizationAccess.canRead(userId, organizationId)) {
      throw new RepositoryImportService.NotAnOrgMemberException(organizationId);
    }
    return repositories.findByOrganization(organizationId);
  }

  /** Fetches one repository, ensuring the caller belongs to its organization. */
  @Transactional(readOnly = true)
  public Repository getForUser(UUID userId, String repositoryId) {
    Repository repo =
        repositories
            .findById(RepositoryId.of(repositoryId))
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId));
    if (!organizationAccess.canRead(userId, repo.organizationId())) {
      throw new RepositoryImportService.NotAnOrgMemberException(repo.organizationId());
    }
    return repo;
  }
}
