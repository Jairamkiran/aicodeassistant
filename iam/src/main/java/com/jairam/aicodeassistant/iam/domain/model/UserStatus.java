package com.jairam.aicodeassistant.iam.domain.model;

/** Lifecycle state of a {@link User}. */
public enum UserStatus {
  /** Normal, may authenticate. */
  ACTIVE,
  /** Administratively disabled; authentication is refused. */
  DISABLED
}
