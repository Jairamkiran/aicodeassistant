package com.jairam.aicodeassistant.iam.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the IAM context's {@link IamAuthProperties} binding. Component scanning discovers the
 * services, adapters, and controllers; this class only registers the typed configuration
 * properties.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamAuthProperties.class)
class IamConfiguration {}
