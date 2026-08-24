package com.jairam.aicodeassistant.iam.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValueObjectsTest {

  @Test
  void emailNormalisesToLowerCaseAndTrims() {
    assertThat(new Email("  User@Example.COM ").value()).isEqualTo("user@example.com");
  }

  @Test
  void emailRejectsMalformedInput() {
    assertThatThrownBy(() -> new Email("not-an-email"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Email("a@b")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Email("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void roleHierarchyIsRespected() {
    assertThat(Role.OWNER.satisfies(Role.ADMIN)).isTrue();
    assertThat(Role.ADMIN.satisfies(Role.ADMIN)).isTrue();
    assertThat(Role.MEMBER.satisfies(Role.ADMIN)).isFalse();
    assertThat(Role.VIEWER.satisfies(Role.MEMBER)).isFalse();
  }

  @Test
  void roleAuthorityIsPrefixed() {
    assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
  }

  @Test
  void slugifyProducesUrlSafeSlug() {
    assertThat(Organization.slugify("Acme Corp, Inc.")).isEqualTo("acme-corp-inc");
    assertThat(Organization.slugify("   ")).isEqualTo("org");
    assertThat(Organization.slugify("Já!!ram")).isEqualTo("j-ram");
  }

  @Test
  void userRegisterStartsActive() {
    var user =
        User.register(
            new Email("a@b.com"), "hash", "Alice", java.time.Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(user.canAuthenticate()).isTrue();
    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void disabledUserCannotAuthenticate() {
    var now = java.time.Instant.parse("2026-01-01T00:00:00Z");
    var user = User.register(new Email("a@b.com"), "hash", "Alice", now);
    user.disable(now);
    assertThat(user.canAuthenticate()).isFalse();
  }
}
