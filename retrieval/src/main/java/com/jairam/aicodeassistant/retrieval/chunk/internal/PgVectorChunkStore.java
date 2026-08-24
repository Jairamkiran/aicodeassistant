package com.jairam.aicodeassistant.retrieval.chunk.internal;

import com.jairam.aicodeassistant.retrieval.chunk.ChunkVectorStore;
import com.jairam.aicodeassistant.retrieval.chunk.CodeChunk;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * pgvector-backed {@link ChunkVectorStore} using {@link JdbcTemplate}.
 *
 * <p>The embedding is written as a pgvector literal ({@code [f1,f2,...]}) cast to {@code vector}.
 * We use plain JDBC (not JPA) here because the {@code vector} type has no standard JPA mapping and
 * the batch INSERT is simple and hot — a hand-written statement is clearer and faster than fighting
 * a converter.
 */
@Component
class PgVectorChunkStore implements ChunkVectorStore {

  private static final int BATCH_SIZE = 100;

  private final JdbcTemplate jdbc;

  PgVectorChunkStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public void upsertAll(List<CodeChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return;
    }
    String sql =
        "INSERT INTO code_chunks "
            + "(repository_id, organization_id, file_path, language, start_line, end_line, "
            + " content, embedding) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS vector))";

    for (int start = 0; start < chunks.size(); start += BATCH_SIZE) {
      List<CodeChunk> batch = chunks.subList(start, Math.min(start + BATCH_SIZE, chunks.size()));
      jdbc.batchUpdate(
          sql,
          batch,
          batch.size(),
          (ps, chunk) -> {
            ps.setObject(1, chunk.repositoryId());
            ps.setObject(2, chunk.organizationId());
            ps.setString(3, chunk.filePath());
            ps.setString(4, chunk.language());
            ps.setInt(5, chunk.startLine());
            ps.setInt(6, chunk.endLine());
            ps.setString(7, chunk.content());
            ps.setString(8, toVectorLiteral(chunk.embedding()));
          });
    }
  }

  @Override
  @Transactional
  public int deleteByRepository(UUID repositoryId) {
    return jdbc.update("DELETE FROM code_chunks WHERE repository_id = ?", repositoryId);
  }

  @Override
  @Transactional(readOnly = true)
  public long countByRepository(UUID repositoryId) {
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM code_chunks WHERE repository_id = ?", Long.class, repositoryId);
    return count == null ? 0L : count;
  }

  /** Formats a float[] as a pgvector literal: {@code [f1,f2,...]}. */
  static String toVectorLiteral(float[] embedding) {
    StringJoiner joiner = new StringJoiner(",", "[", "]");
    for (float v : embedding) {
      joiner.add(Float.toString(v));
    }
    return joiner.toString();
  }
}
