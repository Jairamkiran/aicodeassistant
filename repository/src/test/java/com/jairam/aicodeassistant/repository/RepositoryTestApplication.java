package com.jairam.aicodeassistant.repository;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal bootable app for repository-module tests. Scans the repository module + shared kernel.
 * The cross-module collaborators it depends on ({@code GitHubGateway}, {@code OrganizationAccess})
 * are provided as test doubles by the test, so the other modules need not be on the context.
 */
@SpringBootApplication(
    scanBasePackages = {
      "com.jairam.aicodeassistant.repository",
      "com.jairam.aicodeassistant.platform"
    })
public class RepositoryTestApplication {}
