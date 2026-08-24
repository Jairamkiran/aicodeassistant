package com.jairam.aicodeassistant.retrieval.search.internal;

import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * pgvector cosine KNN over {@code code_chunks}, org-scoped and optionally repository-filtered.
 * Orders by the {@code <=>} cosine-distance operator (which the HNSW index on the embedding column
 * accelerates) and returns the top {@code limit} rows in rank order.
 */
@Component
class VectorSearchDao {

  private static final ChunkRowMapper MAPPER = new ChunkRowMapper(SearchResult.Source.VECTOR);

  private final JdbcTemplate jdbc;

  VectorSearchDao(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Nearest chunks to {@code queryEmbedding} by cosine distance, in rank order. */
  List<SearchResult> search(
      UUID organizationId,
      Optional<UUID> repositoryId,
      Optional<String> language,
      float[] queryEmbedding,
      int limit) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            "SELECT id, repository_id, file_path, language, start_line, end_line, content "
                + "FROM code_chunks WHERE organization_id = ?");
    args.add(organizationId);
    repositoryId.ifPresent(
        id -> {
          sql.append(" AND repository_id = ?");
          args.add(id);
        });
    language.ifPresent(
        lang -> {
          sql.append(" AND lower(language) = ?");
          args.add(lang);
        });
    // Cosine distance ordering; smaller distance = more similar.
    sql.append(" ORDER BY embedding <=> CAST(? AS vector) ASC LIMIT ?");
    args.add(toVectorLiteral(queryEmbedding));
    args.add(limit);

    return jdbc.query(sql.toString(), MAPPER, args.toArray());
  }

  static String toVectorLiteral(float[] embedding) {
    StringJoiner joiner = new StringJoiner(",", "[", "]");
    for (float v : embedding) {
      joiner.add(Float.toString(v));
    }
    return joiner.toString();
  }
}
