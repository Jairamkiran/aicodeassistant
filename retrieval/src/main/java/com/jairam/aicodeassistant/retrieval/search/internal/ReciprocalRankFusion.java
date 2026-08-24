package com.jairam.aicodeassistant.retrieval.search.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion (RRF): combines several ranked result lists into one, scoring each item by
 * {@code sum over lists of 1 / (k + rank)} where rank is 1-based within its list.
 *
 * <p>RRF is the standard, parameter-light way to fuse heterogeneous rankers (here: vector cosine
 * and lexical BM25/ts_rank) whose raw scores are not comparable — it uses only rank position, so no
 * score normalisation is needed. An item appearing high in multiple lists rises to the top. {@code
 * k} (default 60, the value from the original RRF paper) damps the influence of very high ranks.
 *
 * <p>Pure and deterministic — no I/O — so it is fully unit-testable.
 */
public final class ReciprocalRankFusion {

  /** Default damping constant from Cormack et al. (2009). */
  public static final int DEFAULT_K = 60;

  private final int k;

  public ReciprocalRankFusion(int k) {
    if (k <= 0) {
      throw new IllegalArgumentException("k must be > 0");
    }
    this.k = k;
  }

  public static ReciprocalRankFusion withDefaults() {
    return new ReciprocalRankFusion(DEFAULT_K);
  }

  /**
   * Fuses ranked lists of keys into a single ranking (highest fused score first).
   *
   * @param rankedLists each list is in rank order (index 0 = rank 1)
   * @param <K> the item key type (e.g. chunk id)
   * @return keys ordered by descending fused RRF score; ties broken by first appearance for
   *     determinism
   */
  @SafeVarargs
  public final <K> List<Scored<K>> fuse(List<K>... rankedLists) {
    List<WeightedList<K>> weighted = new ArrayList<>(rankedLists.length);
    for (List<K> list : rankedLists) {
      weighted.add(new WeightedList<>(1.0, list));
    }
    return fuseWeighted(weighted);
  }

  /**
   * Weighted RRF: like {@link #fuse} but each list carries a multiplier applied to its rank
   * contributions. A weight &gt; 1 amplifies that retriever's influence, &lt; 1 dampens it — useful
   * when one retriever is empirically more precise for a workload (e.g. lexical for exact
   * identifiers) without changing the parameter-light nature of RRF. Equal weights reproduce plain
   * {@link #fuse}.
   *
   * @param lists weighted ranked lists (each in rank order, index 0 = rank 1); null lists/weights
   *     &le; 0 are ignored
   */
  public <K> List<Scored<K>> fuseWeighted(List<WeightedList<K>> lists) {
    // Preserve first-seen order for stable tie-breaking.
    Map<K, Double> scores = new LinkedHashMap<>();
    for (WeightedList<K> weighted : lists) {
      if (weighted == null || weighted.list() == null || weighted.weight() <= 0) {
        continue;
      }
      List<K> list = weighted.list();
      for (int i = 0; i < list.size(); i++) {
        K key = list.get(i);
        double contribution = weighted.weight() / (k + (i + 1));
        scores.merge(key, contribution, Double::sum);
      }
    }
    List<Scored<K>> fused = new ArrayList<>(scores.size());
    scores.forEach((key, score) -> fused.add(new Scored<>(key, score)));
    // Stable sort by score desc; equal scores keep insertion order (first-seen).
    fused.sort(Comparator.comparingDouble(Scored<K>::score).reversed());
    return fused;
  }

  /** A ranked list paired with its fusion weight. */
  public record WeightedList<K>(double weight, List<K> list) {}

  /** A key with its fused score. */
  public record Scored<K>(K key, double score) {}
}
