package com.jairam.aicodeassistant.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.model.Role;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.port.MembershipRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies the {@code org-membership} cache actually short-circuits repository reads and that
 * eviction restores a fresh read. Uses a real Spring caching proxy over {@link MembershipLookup}
 * with a counting fake repository — no Docker, no database.
 */
class MembershipLookupCachingTest {

  private static final UUID USER = UUID.randomUUID();
  private static final UUID ORG = UUID.randomUUID();

  private AnnotationConfigApplicationContext context;
  private CountingMembershipRepository repository;
  private MembershipLookup lookup;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
    repository = context.getBean(CountingMembershipRepository.class);
    lookup = context.getBean(MembershipLookup.class);
    repository.reset();
  }

  @Test
  void secondLookupIsServedFromCache() {
    lookup.role(USER, ORG);
    lookup.role(USER, ORG);
    lookup.role(USER, ORG);

    assertThat(repository.calls()).isEqualTo(1);
  }

  @Test
  void evictionForcesAFreshRead() {
    lookup.role(USER, ORG);
    lookup.evict(USER, ORG);
    lookup.role(USER, ORG);

    assertThat(repository.calls()).isEqualTo(2);
  }

  @Test
  void distinctKeysAreCachedIndependently() {
    lookup.role(USER, ORG);
    lookup.role(UUID.randomUUID(), ORG);

    assertThat(repository.calls()).isEqualTo(2);
  }

  @Configuration
  @EnableCaching
  static class CachingTestConfig {

    @Bean
    CacheManager cacheManager() {
      CaffeineCacheManager manager = new CaffeineCacheManager("org-membership");
      manager.setCaffeine(Caffeine.newBuilder().maximumSize(1_000));
      return manager;
    }

    @Bean
    CountingMembershipRepository membershipRepository() {
      return new CountingMembershipRepository();
    }

    @Bean
    MembershipLookup membershipLookup(CountingMembershipRepository repository) {
      return new MembershipLookup(repository);
    }
  }

  /** Fake repository counting how many times the backing store is queried. */
  static final class CountingMembershipRepository implements MembershipRepository {
    private final AtomicInteger calls = new AtomicInteger();

    int calls() {
      return calls.get();
    }

    void reset() {
      calls.set(0);
    }

    @Override
    public Optional<Membership> findByUserAndOrganization(
        UserId userId, OrganizationId organizationId) {
      calls.incrementAndGet();
      return Optional.of(Membership.grant(userId, organizationId, Role.MEMBER, Instant.EPOCH));
    }

    @Override
    public Membership save(Membership membership) {
      return membership;
    }

    @Override
    public java.util.List<Membership> findByUser(UserId userId) {
      return java.util.List.of();
    }

    @Override
    public java.util.List<Membership> findByOrganization(OrganizationId organizationId) {
      return java.util.List.of();
    }
  }
}
