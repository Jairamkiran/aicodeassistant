package com.jairam.aicodeassistant.retrieval.search.internal;

import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Postgres full-text search over {@code code_chunks.content_fts}, org-scoped and optionally
 * repository-filtered. Uses {@code plainto_tsquery} (treats the input as plain words, robust to
 * punctuation in code queries) matched with {@code @@}, ranked by {@code ts_rank}. The GIN index on
 * {@code content_fts} accelerates it.
 */
@Component
class LexicalSearchDao {

  private static final ChunkRowMapper MAPPER = new ChunkRowMapper(SearchResult.Source.LEXICAL);

  private final JdbcTemplate jdbc;

  LexicalSearchDao(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Best full-text matches for {@code text}, in rank order. */
  List<SearchResult> search(
      UUID organizationId,
      Optional<UUID> repositoryId,
      Optional<String> language,
      String text,
      int limit) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            "SELECT id, repository_id, file_path, language, start_line, end_line, content "
                + "FROM code_chunks "
                + "WHERE organization_id = ? "
                + "AND content_fts @@ plainto_tsquery('english', ?)");
    args.add(organizationId);
    args.add(text);
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
    sql.append(" ORDER BY ts_rank(content_fts, plainto_tsquery('english', ?)) DESC LIMIT ?");
    args.add(text);
    args.add(limit);

    return jdbc.query(sql.toString(), MAPPER, args.toArray());
  }
}
