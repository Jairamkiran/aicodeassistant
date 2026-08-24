package com.jairam.aicodeassistant.retrieval.search.internal;

import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

/**
 * Maps a {@code code_chunks} row to a {@link SearchResult}. The relevance score/source are set by
 * the fusing service afterward, so this mapper stores a neutral placeholder score and the given
 * source. Shared by the vector and lexical DAOs.
 */
final class ChunkRowMapper implements RowMapper<SearchResult> {

  private final SearchResult.Source source;

  ChunkRowMapper(SearchResult.Source source) {
    this.source = source;
  }

  @Override
  public SearchResult mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new SearchResult(
        rs.getObject("id", UUID.class),
        rs.getObject("repository_id", UUID.class),
        rs.getString("file_path"),
        rs.getString("language"),
        rs.getInt("start_line"),
        rs.getInt("end_line"),
        rs.getString("content"),
        0.0, // fused score assigned later
        source);
  }
}
