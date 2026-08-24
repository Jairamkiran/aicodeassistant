package com.jairam.aicodeassistant.iam.domain.port;

import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import java.util.List;
import java.util.Optional;

/** Outbound port for persisting and loading {@link Membership} aggregates. */
public interface MembershipRepository {

  Membership save(Membership membership);

  Optional<Membership> findByUserAndOrganization(UserId userId, OrganizationId organizationId);

  List<Membership> findByUser(UserId userId);

  List<Membership> findByOrganization(OrganizationId organizationId);
}
