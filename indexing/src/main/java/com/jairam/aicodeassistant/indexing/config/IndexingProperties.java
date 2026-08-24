package com.jairam.aicodeassistant.indexing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Indexing configuration, bound from {@code aicodeassistant.indexing}.
 *
 * @param linesPerChunk chunk window size in lines
 * @param overlapLines overlap between consecutive chunks
 * @param maxFileBytes files larger than this are skipped by the cloner (binary/huge)
 */
@ConfigurationProperties(prefix = "aicodeassistant.indexing")
public record IndexingProperties(int linesPerChunk, int overlapLines, long maxFileBytes) {

  public IndexingProperties {
    if (linesPerChunk <= 0) {
      linesPerChunk = 60;
    }
    if (overlapLines < 0 || overlapLines >= linesPerChunk) {
      overlapLines = 10;
    }
    if (maxFileBytes <= 0) {
      maxFileBytes = 1_000_000; // 1 MB — skip larger files (likely generated/binary)
    }
  }
}
