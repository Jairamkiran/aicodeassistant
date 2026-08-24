package com.jairam.aicodeassistant.iam.domain.port;

import com.jairam.aicodeassistant.iam.domain.model.Organization;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import java.util.Optional;

/** Outbound port for persisting and loading {@link Organization} aggregates. */
public interface OrganizationRepository {

  Organization save(Organization organization);

  Optional<Organization> findById(OrganizationId id);

  boolean existsBySlug(String slug);
}
