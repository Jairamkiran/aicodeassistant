/**
 * Repository bounded context.
 *
 * <p>Spring Modulith application module owning repository registration and the import lifecycle. It
 * depends on the {@code integration} module's public GitHub gateway and the {@code iam} module's
 * public {@code OrganizationAccess} port — both declared below so the cross-context dependencies
 * are explicit and boundary-verified. Provider types and credentials never reach this module.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Repository",
    allowedDependencies = {"integration :: github", "iam :: api", "platform"})
package com.jairam.aicodeassistant.repository;
