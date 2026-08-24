package com.jairam.aicodeassistant.indexing.config;

import com.jairam.aicodeassistant.indexing.domain.Chunker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the indexing context: properties + the {@link Chunker} bean. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IndexingProperties.class)
public class IndexingConfig {

  @Bean
  Chunker chunker(IndexingProperties properties) {
    return new Chunker(properties.linesPerChunk(), properties.overlapLines());
  }
}
