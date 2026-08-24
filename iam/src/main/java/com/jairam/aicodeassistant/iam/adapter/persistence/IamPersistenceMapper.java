package com.jairam.aicodeassistant.iam.adapter.persistence;

import com.jairam.aicodeassistant.iam.domain.apikey.ApiKey;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyId;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyScope;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyStatus;
import com.jairam.aicodeassistant.iam.domain.model.Email;
import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.MembershipId;
import com.jairam.aicodeassistant.iam.domain.model.Organization;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.model.RefreshToken;
import com.jairam.aicodeassistant.iam.domain.model.Role;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.model.UserStatus;

/**
 * Translates between domain aggregates and JPA entities.
 *
 * <p>Hand-written on purpose: the mappings wrap/unwrap value objects (typed ids, {@link Email},
 * enums) which a generic mapper handles awkwardly, and the code is short enough that explicitness
 * beats a framework here.
 */
final class IamPersistenceMapper {

  private IamPersistenceMapper() {}

  // --- User --------------------------------------------------------------------
  static UserEntity toEntity(User user) {
    return new UserEntity(
        user.id().value(),
        user.email().value(),
        user.passwordHash(),
        user.displayName(),
        user.status().name(),
        user.createdAt(),
        user.updatedAt(),
        user.version());
  }

  static User toDomain(UserEntity e) {
    return User.rehydrate(
        new UserId(e.getId()),
        new Email(e.getEmail()),
        e.getPasswordHash(),
        e.getDisplayName(),
        UserStatus.valueOf(e.getStatus()),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }

  // --- Organization ------------------------------------------------------------
  static OrganizationEntity toEntity(Organization org) {
    return new OrganizationEntity(
        org.id().value(), org.name(), org.slug(), org.createdAt(), org.updatedAt(), org.version());
  }

  static Organization toDomain(OrganizationEntity e) {
    return Organization.rehydrate(
        new OrganizationId(e.getId()),
        e.getName(),
        e.getSlug(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }

  // --- Membership --------------------------------------------------------------
  static MembershipEntity toEntity(Membership m) {
    return new MembershipEntity(
        m.id().value(),
        m.userId().value(),
        m.organizationId().value(),
        m.role().name(),
        m.createdAt(),
        m.version());
  }

  static Membership toDomain(MembershipEntity e) {
    return Membership.rehydrate(
        new MembershipId(e.getId()),
        new UserId(e.getUserId()),
        new OrganizationId(e.getOrganizationId()),
        Role.valueOf(e.getRole()),
        e.getCreatedAt(),
        e.getVersion());
  }

  // --- ApiKey ------------------------------------------------------------------
  static ApiKeyEntity toEntity(ApiKey k) {
    return new ApiKeyEntity(
        k.id().value(),
        k.userId().value(),
        k.name(),
        k.keyPrefix(),
        k.secretHash(),
        ApiKeyScope.toCsv(k.scopes()),
        k.status().name(),
        k.createdAt(),
        k.expiresAt(),
        k.lastUsedAt(),
        k.revokedAt());
  }

  static ApiKey toDomain(ApiKeyEntity e) {
    return ApiKey.rehydrate(
        new ApiKeyId(e.getId()),
        new UserId(e.getUserId()),
        e.getName(),
        e.getKeyPrefix(),
        e.getSecretHash(),
        ApiKeyScope.parse(e.getScopes()),
        ApiKeyStatus.valueOf(e.getStatus()),
        e.getCreatedAt(),
        e.getExpiresAt(),
        e.getLastUsedAt(),
        e.getRevokedAt());
  }

  // --- RefreshToken ------------------------------------------------------------
  static RefreshTokenEntity toEntity(RefreshToken t) {
    return new RefreshTokenEntity(
        t.id(),
        t.userId().value(),
        t.familyId(),
        t.tokenHash(),
        t.issuedAt(),
        t.expiresAt(),
        t.usedAt(),
        t.revokedAt());
  }

  static RefreshToken toDomain(RefreshTokenEntity e) {
    return RefreshToken.rehydrate(
        e.getId(),
        new UserId(e.getUserId()),
        e.getFamilyId(),
        e.getTokenHash(),
        e.getIssuedAt(),
        e.getExpiresAt(),
        e.getUsedAt(),
        e.getRevokedAt());
  }
}
