package com.jairam.aicodeassistant.audit.domain;

/** The kind of principal that performed an audited action. */
public enum ActorType {
  /** An authenticated end user. */
  USER,
  /** An API key acting on a user's behalf. */
  API_KEY,
  /** Unauthenticated / pre-authentication (e.g. a failed login attempt). */
  ANONYMOUS,
  /** The system itself (background process). */
  SYSTEM
}
