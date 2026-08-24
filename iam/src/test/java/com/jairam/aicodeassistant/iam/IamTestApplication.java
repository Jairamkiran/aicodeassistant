package com.jairam.aicodeassistant.iam;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal bootable application for iam module tests.
 *
 * <p>iam is a library (no {@code main}), so its slice/e2e tests need a boot entry point. This scans
 * only the iam base package plus the shared kernel, giving a realistic — but self-contained —
 * context: JPA repositories, security filter chain, controllers, and services, backed by H2 (see
 * {@code application-iamtest.yml}). It deliberately does not pull in the other bounded contexts.
 */
@SpringBootApplication(
    scanBasePackages = {"com.jairam.aicodeassistant.iam", "com.jairam.aicodeassistant.platform"})
public class IamTestApplication {}
