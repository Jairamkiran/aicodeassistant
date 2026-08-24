package com.jairam.aicodeassistant.retrieval.search.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables hybrid-search ranking tuning properties. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RetrievalProperties.class)
class RetrievalConfig {}
