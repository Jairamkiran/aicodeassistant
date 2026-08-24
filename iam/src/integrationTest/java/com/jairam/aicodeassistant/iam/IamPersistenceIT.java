package com.jairam.aicodeassistant.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.iam.domain.model.Email;
import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.Organization;
import com.jairam.aicodeassistant.iam.domain.model.RefreshToken;
import com.jairam.aicodeassistant.iam.domain.model.Role;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.port.MembershipRepository;
import com.jairam.aicodeassistant.iam.domain.port.OrganizationRepository;
import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenStore;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the iam JPA mappings and port adapters against a REAL PostgreSQL instance
 * (Testcontainers), which H2 cannot fully substitute for — Postgres {@code TIMESTAMPTZ}, {@code
 * UUID}, and unique constraints behave differently.
 *
 * <p>Uses Hibernate schema generation ({@code ddl-auto=create-drop}) so this test focuses on
 * object/relational mapping correctness; the Flyway migration itself is validated by the app
 * module's infrastructure integration test. Requires a Docker daemon; runs under {@code ./gradlew
 * integrationTest} in CI.
 */
@SpringBootTest
@Testcontainers
class IamPersistenceIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("iam")
          .withUsername("iam")
          .withPassword("iam");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.flyway.enabled", () -> "false");
  }

  @Autowired private UserRepository users;
  @Autowired private OrganizationRepository organizations;
  @Autowired private MembershipRepository memberships;
  @Autowired private RefreshTokenStore refreshTokens;

  @Test
  void userRoundTripsThroughPostgres() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    User saved =
        users.save(User.register(new Email("pg@example.com"), "{bcrypt}$2aFAKE", "PG User", now));

    var loaded = users.findByEmail(new Email("pg@example.com"));
    assertThat(loaded).isPresent();
    assertThat(loaded.get().id()).isEqualTo(saved.id());
    assertThat(users.existsByEmail(new Email("PG@example.com"))).isTrue();
  }

  @Test
  void membershipAndOrganizationRoundTrip() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    User user =
        users.save(User.register(new Email("owner@example.com"), "{bcrypt}$2aFAKE", "Owner", now));
    Organization org = organizations.save(Organization.create("Acme Corp", now));

    memberships.save(Membership.grant(user.id(), org.id(), Role.OWNER, now));

    var found = memberships.findByUserAndOrganization(user.id(), org.id());
    assertThat(found).isPresent();
    assertThat(found.get().role()).isEqualTo(Role.OWNER);
    assertThat(organizations.findById(org.id())).isPresent();
  }

  @Test
  void refreshTokenFamilyRevocationWorksOnPostgres() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    User user =
        users.save(User.register(new Email("rt@example.com"), "{bcrypt}$2aFAKE", "RT", now));

    RefreshToken t1 =
        refreshTokens.save(
            RefreshToken.issueNewFamily(user.id(), "hash-1", now, now.plusSeconds(3600)));
    refreshTokens.save(
        RefreshToken.issueInFamily(user.id(), t1.familyId(), "hash-2", now, now.plusSeconds(3600)));

    int revoked = refreshTokens.revokeFamily(t1.familyId(), now.plusSeconds(1));
    assertThat(revoked).isEqualTo(2);

    var reloaded = refreshTokens.findByTokenHash("hash-1");
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().isRevoked()).isTrue();
  }
}
