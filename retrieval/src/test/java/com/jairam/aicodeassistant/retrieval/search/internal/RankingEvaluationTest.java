package com.jairam.aicodeassistant.retrieval.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.retrieval.search.internal.ReciprocalRankFusion.Scored;
import com.jairam.aicodeassistant.retrieval.search.internal.ReciprocalRankFusion.WeightedList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A small, self-contained retrieval-ranking evaluation harness (M9 evaluation framework). It runs a
 * fixed set of query fixtures — each with a known-relevant chunk and simulated per-retriever
 * rankings — through fusion and asserts an aggregate <b>recall@k</b> and <b>mean reciprocal rank
 * (MRR)</b> threshold. This guards against regressions in the fusion math and lets us compare
 * ranking strategies (e.g. weighting) with a repeatable, deterministic metric — no Docker, no LLM.
 */
class RankingEvaluationTest {

  /** A labelled retrieval scenario: two retriever rankings + the id that is truly relevant. */
  private record Fixture(String name, List<String> vector, List<String> lexical, String relevant) {}

  private static final List<Fixture> FIXTURES =
      List.of(
          // Exact-identifier query: lexical nails it, vector buries it — hybrid should recover.
          new Fixture(
              "rare-identifier",
              List.of("noise-1", "noise-2", "noise-3", "hit"),
              List.of("hit", "noise-4"),
              "hit"),
          // Semantic/paraphrase query: vector finds it, lexical misses entirely.
          new Fixture(
              "paraphrase", List.of("hit", "noise-a"), List.of("noise-b", "noise-c"), "hit"),
          // Both retrievers agree at rank 1.
          new Fixture("consensus", List.of("hit", "x"), List.of("hit", "y"), "hit"),
          // Relevant item is mid-pack in both — still should land within top-3.
          new Fixture("mid-pack", List.of("n1", "hit", "n2"), List.of("n3", "hit", "n4"), "hit"));

  private final ReciprocalRankFusion rrf = ReciprocalRankFusion.withDefaults();

  @Test
  @DisplayName("Unweighted fusion achieves perfect recall@3 and strong MRR over the fixtures")
  void recallAndMrrMeetThreshold() {
    int k = 3;
    int hits = 0;
    double reciprocalRankSum = 0.0;

    for (Fixture f : FIXTURES) {
      List<Scored<String>> fused = rrf.fuse(f.vector(), f.lexical());
      int rank = rankOf(fused, f.relevant());
      if (rank > 0 && rank <= k) {
        hits++;
      }
      if (rank > 0) {
        reciprocalRankSum += 1.0 / rank;
      }
    }

    double recallAtK = (double) hits / FIXTURES.size();
    double mrr = reciprocalRankSum / FIXTURES.size();

    assertThat(recallAtK).as("recall@%d", k).isEqualTo(1.0);
    assertThat(mrr).as("mean reciprocal rank").isGreaterThan(0.6);
  }

  @Test
  @DisplayName("Boosting the lexical weight lifts an exact-match above a vector-favoured tie")
  void lexicalWeightBreaksTieTowardExactMatch() {
    // Both retrievers surface their pick at rank 1; unweighted fusion ties (first-seen wins).
    List<String> vector = List.of("semantic", "hit");
    List<String> lexical = List.of("hit", "semantic");

    // Unweighted: 'semantic' and 'hit' both = 1/61 + 1/62; first-seen ('semantic') leads.
    assertThat(rrf.fuse(vector, lexical).get(0).key()).isEqualTo("semantic");

    // Weighting lexical higher promotes the exact-match 'hit' to the top.
    List<Scored<String>> weighted =
        rrf.fuseWeighted(
            List.of(new WeightedList<>(1.0, vector), new WeightedList<>(2.0, lexical)));
    assertThat(weighted.get(0).key()).isEqualTo("hit");
  }

  private static int rankOf(List<Scored<String>> ranking, String key) {
    for (int i = 0; i < ranking.size(); i++) {
      if (ranking.get(i).key().equals(key)) {
        return i + 1; // 1-based
      }
    }
    return -1;
  }
}
