package com.jairam.aicodeassistant.repository.domain;

/**
 * Import lifecycle of a repository.
 *
 * <p>M3 registers a repo as {@code REGISTERED} and requests import. The indexing worker + saga (M4)
 * advances it {@code REGISTERED → IMPORTING → READY}, or to {@code FAILED} with a reason on a
 * compensated failure.
 */
public enum ImportStatus {
  REGISTERED,
  IMPORTING,
  READY,
  FAILED
}
