package com.jairam.aicodeassistant.retrieval.search.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hybrid-search ranking tuning, bound from {@code aicodeassistant.retrieval}.
 *
 * <p>The fusion weights let an operator bias reciprocal-rank fusion toward one retriever without
 * touching code — e.g. raise {@code lexicalWeight} for codebases dominated by exact identifiers.
 * Equal weights (the default) reproduce classic unweighted RRF.
 *
 * @param rrfK RRF damping constant (higher = flatter ranking influence)
 * @param vectorWeight fusion weight for the semantic (pgvector) retriever
 * @param lexicalWeight fusion weight for the lexical (Postgres FTS) retriever
 */
@ConfigurationProperties(prefix = "aicodeassistant.retrieval")
public record RetrievalProperties(int rrfK, double vectorWeight, double lexicalWeight) {

  public RetrievalProperties {
    if (rrfK <= 0) {
      rrfK = ReciprocalRankFusion.DEFAULT_K;
    }
    if (vectorWeight <= 0) {
      vectorWeight = 1.0;
    }
    if (lexicalWeight <= 0) {
      lexicalWeight = 1.0;
    }
  }
}
