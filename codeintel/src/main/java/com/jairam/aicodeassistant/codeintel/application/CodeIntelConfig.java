package com.jairam.aicodeassistant.codeintel.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables code-intelligence tuning properties. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CodeIntelProperties.class)
public class CodeIntelConfig {}
