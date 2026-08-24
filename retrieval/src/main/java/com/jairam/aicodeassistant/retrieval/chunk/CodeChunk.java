package com.jairam.aicodeassistant.retrieval.chunk;

import java.util.Objects;
import java.util.UUID;

/**
 * A chunk of a repository file plus its embedding — the unit stored for retrieval. Produced by the
 * indexing saga (M4) and queried for RAG (M5).
 *
 * @param repositoryId owning repository
 * @param organizationId owning organization (for tenant-scoped search)
 * @param filePath path of the source file within the repo
 * @param language detected language tag (by extension), or null
 * @param startLine first line of the chunk (1-based, inclusive)
 * @param endLine last line of the chunk (inclusive)
 * @param content the chunk text
 * @param embedding the embedding vector (length must equal the store's dimension)
 */
public record CodeChunk(
    UUID repositoryId,
    UUID organizationId,
    String filePath,
    String language,
    int startLine,
    int endLine,
    String content,
    float[] embedding) {

  public CodeChunk {
    Objects.requireNonNull(repositoryId, "repositoryId");
    Objects.requireNonNull(organizationId, "organizationId");
    Objects.requireNonNull(filePath, "filePath");
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(embedding, "embedding");
  }
}
