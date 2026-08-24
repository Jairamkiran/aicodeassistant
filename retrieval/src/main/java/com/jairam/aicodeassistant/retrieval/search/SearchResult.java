package com.jairam.aicodeassistant.retrieval.search;

import java.util.UUID;

/**
 * A single search hit with file:line provenance and a fused relevance score.
 *
 * @param chunkId the code_chunks row id
 * @param repositoryId owning repository
 * @param filePath source file path within the repo
 * @param language detected language tag, or null
 * @param startLine first line of the chunk (1-based, inclusive)
 * @param endLine last line of the chunk (inclusive)
 * @param content the chunk text (callers may snippet it for display)
 * @param score fused relevance score (higher = more relevant)
 * @param source which retriever(s) surfaced this hit
 */
public record SearchResult(
    UUID chunkId,
    UUID repositoryId,
    String filePath,
    String language,
    int startLine,
    int endLine,
    String content,
    double score,
    Source source) {

  /** Which retrieval path surfaced a result. */
  public enum Source {
    VECTOR,
    LEXICAL,
    HYBRID
  }
}
