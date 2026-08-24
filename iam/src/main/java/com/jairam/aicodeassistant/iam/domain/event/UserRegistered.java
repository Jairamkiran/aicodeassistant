package com.jairam.aicodeassistant.iam.domain.event;

import com.jairam.aicodeassistant.platform.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new user completes registration. Other contexts (e.g. notification for a welcome
 * email, analytics for signups) may subscribe.
 *
 * @param eventId unique id for this occurrence
 * @param occurredAt when registration happened
 * @param userId the new user's id
 * @param email the registered email
 */
public record UserRegistered(UUID eventId, Instant occurredAt, UUID userId, String email)
    implements DomainEvent {

  /** Factory stamping a fresh event id. */
  public static UserRegistered of(UUID userId, String email, Instant occurredAt) {
    return new UserRegistered(UUID.randomUUID(), occurredAt, userId, email);
  }
}
