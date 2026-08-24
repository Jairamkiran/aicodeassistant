/**
 * Identity &amp; Access bounded context.
 *
 * <p>Spring Modulith application module. Owns users, organizations, memberships (RBAC),
 * authentication (JWT access tokens + rotating refresh-token families), and registration.
 *
 * <p>Internally organised hexagonally: {@code domain} (aggregates, value objects, events, ports —
 * pure Java), {@code application} (use-case services), and {@code adapter.*} (inbound REST,
 * outbound JPA/security/crypto). Only this base package and its public sub-packages form the
 * module's API; other modules must not reach into implementation types.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Identity & Access")
package com.jairam.aicodeassistant.iam;
