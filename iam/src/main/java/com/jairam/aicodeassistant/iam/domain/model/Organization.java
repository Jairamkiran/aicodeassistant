package com.jairam.aicodeassistant.iam.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Organization aggregate root — a tenant that users belong to via {@link Membership}. Holds a
 * display {@code name} and a URL-safe {@code slug} derived from it (unique per the persistence
 * constraint).
 */
public final class Organization {

  private final OrganizationId id;
  private String name;
  private final String slug;
  private final Instant createdAt;
  private Instant updatedAt;
  private final long version;

  private Organization(
      OrganizationId id,
      String name,
      String slug,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireText(name, "name");
    this.slug = requireText(slug, "slug");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /** Creates a new organization, deriving a slug from the name. */
  public static Organization create(String name, Instant now) {
    String cleaned = requireText(name, "name");
    return new Organization(OrganizationId.newId(), cleaned, slugify(cleaned), now, now, 0L);
  }

  /** Reconstructs an organization loaded from persistence. */
  public static Organization rehydrate(
      OrganizationId id,
      String name,
      String slug,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    return new Organization(id, name, slug, createdAt, updatedAt, version);
  }

  /**
   * Converts a display name into a URL-safe slug: lower-cased, non-alphanumerics collapsed to
   * single hyphens, trimmed. Falls back to {@code "org"} if the name has no usable characters (the
   * unique constraint still applies).
   */
  public static String slugify(String value) {
    String slug =
        value
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+)|(-+$)", "");
    return slug.isEmpty() ? "org" : slug;
  }

  public OrganizationId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String slug() {
    return slug;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof Organization other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
