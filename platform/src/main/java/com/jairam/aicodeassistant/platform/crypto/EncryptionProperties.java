package com.jairam.aicodeassistant.platform.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for {@link EncryptionService}, bound from {@code aicodeassistant.crypto}.
 *
 * @param key Base64-encoded 256-bit AES key. In production this MUST be supplied via a secret (env
 *     var / K8s secret); the development fallback in application.yml is clearly non-production and
 *     must be overridden.
 */
@ConfigurationProperties(prefix = "aicodeassistant.crypto")
public record EncryptionProperties(String key) {}
