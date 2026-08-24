package com.jairam.aicodeassistant.platform.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Provides an {@link EncryptionService} (AES-256-GCM) when a key is configured.
 *
 * <p>Guarded by {@code aicodeassistant.crypto.key} being present so modules that never encrypt
 * anything (and deployables without the property) are unaffected. The key comes from
 * configuration/secret; see {@link EncryptionProperties}.
 */
@AutoConfiguration
@EnableConfigurationProperties(EncryptionProperties.class)
public class PlatformCryptoAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(EncryptionService.class)
  @ConditionalOnProperty(prefix = "aicodeassistant.crypto", name = "key")
  EncryptionService encryptionService(EncryptionProperties properties) {
    return new AesGcmEncryptionService(properties.key());
  }
}
