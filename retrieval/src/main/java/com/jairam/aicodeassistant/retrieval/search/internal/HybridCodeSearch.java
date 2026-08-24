package com.jairam.aicodeassistant.retrieval.search.internal;

import com.jairam.aicodeassistant.ai.embedding.EmbeddingClient;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hybrid {@link CodeSearch}: runs pgvector cosine KNN and Postgres full-text search, then fuses
 * their rankings with {@link ReciprocalRankFusion}.
 *
 * <p>Combining a semantic retriever (vector) with an exact-token retriever (lexical) is what makes
 * code search work well: vectors catch paraphrase/intent, lexical catches identifiers, error
 * strings, and rare symbols. RRF needs no score normalisation between the two. A chunk found by
 * both retrievers is marked {@link SearchResult.Source#HYBRID} and naturally scores highest.
 *
 * <p>If the embedding provider is unavailable, search degrades to lexical-only rather than failing
 * — availability over completeness for a read path.
 */
@Service
class HybridCodeSearch implements CodeSearch {

  private static final Logger log = LoggerFactory.getLogger(HybridCodeSearch.class);

  private final VectorSearchDao vectorDao;
  private final LexicalSearchDao lexicalDao;
  private final EmbeddingClient embeddingClient;
  private final MeterRegistry metrics;
  private final ReciprocalRankFusion rrf;
  private final double vectorWeight;
  private final double lexicalWeight;

  HybridCodeSearch(
      VectorSearchDao vectorDao,
      LexicalSearchDao lexicalDao,
      EmbeddingClient embeddingClient,
      MeterRegistry metrics,
      RetrievalProperties properties) {
    this.vectorDao = vectorDao;
    this.lexicalDao = lexicalDao;
    this.embeddingClient = embeddingClient;
    this.metrics = metrics;
    this.rrf = new ReciprocalRankFusion(properties.rrfK());
    this.vectorWeight = properties.vectorWeight();
    this.lexicalWeight = properties.lexicalWeight();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SearchResult> search(SearchQuery query) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      return doSearch(query);
    } finally {
      sample.stop(
          Timer.builder("retrieval.search")
              .description("Hybrid code-search latency")
              .tag("scoped", String.valueOf(query.repositoryFilter().isPresent()))
              .register(metrics));
    }
  }

  private List<SearchResult> doSearch(SearchQuery query) {
    // Over-fetch from each retriever so fusion has enough candidates.
    int perRetriever = Math.min(query.limit() * 3, SearchQuery.MAX_LIMIT * 3);

    List<SearchResult> lexical =
        lexicalDao.search(
            query.organizationId(),
            query.repositoryFilter(),
            query.languageFilter(),
            query.text(),
            perRetriever);

    List<SearchResult> vector = vectorSearchOrEmpty(query, perRetriever);

    // Index results by chunk id for lookup after fusion; remember which sources hit.
    Map<UUID, SearchResult> byId = new LinkedHashMap<>();
    Map<UUID, Boolean> inVector = new LinkedHashMap<>();
    Map<UUID, Boolean> inLexical = new LinkedHashMap<>();
    vector.forEach(
        r -> {
          byId.putIfAbsent(r.chunkId(), r);
          inVector.put(r.chunkId(), true);
        });
    lexical.forEach(
        r -> {
          byId.putIfAbsent(r.chunkId(), r);
          inLexical.put(r.chunkId(), true);
        });

    List<ReciprocalRankFusion.Scored<UUID>> fused =
        rrf.fuseWeighted(
            List.of(
                new ReciprocalRankFusion.WeightedList<>(vectorWeight, idsOf(vector)),
                new ReciprocalRankFusion.WeightedList<>(lexicalWeight, idsOf(lexical))));

    return fused.stream()
        .limit(query.limit())
        .map(
            scored -> {
              SearchResult base = byId.get(scored.key());
              boolean both =
                  inVector.getOrDefault(scored.key(), false)
                      && inLexical.getOrDefault(scored.key(), false);
              SearchResult.Source source = both ? SearchResult.Source.HYBRID : base.source();
              return new SearchResult(
                  base.chunkId(),
                  base.repositoryId(),
                  base.filePath(),
                  base.language(),
                  base.startLine(),
                  base.endLine(),
                  base.content(),
                  scored.score(),
                  source);
            })
        .toList();
  }

  private List<SearchResult> vectorSearchOrEmpty(SearchQuery query, int perRetriever) {
    try {
      float[] embedding = embeddingClient.embed(query.text());
      return vectorDao.search(
          query.organizationId(),
          query.repositoryFilter(),
          query.languageFilter(),
          embedding,
          perRetriever);
    } catch (RuntimeException e) {
      // Degrade to lexical-only if embeddings are unavailable.
      log.warn("Vector search unavailable, falling back to lexical-only: {}", e.getMessage());
      return List.of();
    }
  }

  private static List<UUID> idsOf(List<SearchResult> results) {
    return results.stream().map(SearchResult::chunkId).toList();
  }
}
