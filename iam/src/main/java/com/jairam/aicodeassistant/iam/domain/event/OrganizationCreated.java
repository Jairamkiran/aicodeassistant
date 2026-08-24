package com.jairam.aicodeassistant.iam.domain.event;

import com.jairam.aicodeassistant.platform.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new organization is created (with its creator as OWNER).
 *
 * @param eventId unique id for this occurrence
 * @param occurredAt when the organization was created
 * @param organizationId the new organization's id
 * @param ownerUserId the creating user, granted OWNER
 */
public record OrganizationCreated(
    UUID eventId, Instant occurredAt, UUID organizationId, UUID ownerUserId)
    implements DomainEvent {

  public static OrganizationCreated of(UUID organizationId, UUID ownerUserId, Instant occurredAt) {
    return new OrganizationCreated(UUID.randomUUID(), occurredAt, organizationId, ownerUserId);
  }
}
