package com.jairam.aicodeassistant.indexing.domain;

/**
 * Saga state of an index job. Ordered along the happy path; {@link #FAILED} is the terminal
 * compensation state and {@link #INDEXED} the terminal success.
 */
public enum IndexStatus {
  REGISTERED,
  CLAIMED,
  CLONING,
  PARSING,
  EMBEDDING,
  UPSERTING,
  INDEXED,
  FAILED
}
