package com.jairam.aicodeassistant.retrieval.search.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jairam.aicodeassistant.retrieval.search.internal.ReciprocalRankFusion.Scored;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for reciprocal-rank fusion — pure logic, no infrastructure. Proves ranking math, dedup
 * across lists, the "found by both retrievers ranks highest" property, and that hybrid fusion beats
 * a single retriever on a fixture with a known-relevant item.
 */
class ReciprocalRankFusionTest {

  private final ReciprocalRankFusion rrf = new ReciprocalRankFusion(60);

  @Test
  void itemInBothListsScoresHigherThanItemInOne() {
    // "shared" is rank 2 in vector and rank 1 in lexical → two contributions.
    // "vOnly" is rank 1 in vector only; "lOnly" is rank 2 in lexical only.
    List<String> vector = List.of("vOnly", "shared");
    List<String> lexical = List.of("shared", "lOnly");

    List<Scored<String>> fused = rrf.fuse(vector, lexical);

    assertThat(fused.get(0).key()).isEqualTo("shared");
    // shared = 1/(60+2) + 1/(60+1) ≈ 0.03252; vOnly = 1/61 ≈ 0.01639
    assertThat(fused.get(0).score()).isGreaterThan(fused.get(1).score());
  }

  @Test
  void dedupsAcrossListsIntoSingleEntry() {
    List<Scored<String>> fused = rrf.fuse(List.of("a", "b"), List.of("b", "a"), List.of("a"));
    assertThat(fused).extracting(Scored::key).containsExactlyInAnyOrder("a", "b");
    // 'a' appears in all three lists → highest.
    assertThat(fused.get(0).key()).isEqualTo("a");
  }

  @Test
  void higherRankContributesMore() {
    // Same item at rank 1 vs rank 5 → rank 1 scores higher.
    double rank1 = rrf.fuse(List.of("x")).get(0).score();
    double rank5 = rrf.fuse(List.of("a", "b", "c", "d", "x")).get(0).score();
    // In the second list 'x' is last; compare its own contribution.
    Scored<String> xAt5 =
        rrf.fuse(List.of("a", "b", "c", "d", "x")).stream()
            .filter(s -> s.key().equals("x"))
            .findFirst()
            .orElseThrow();
    assertThat(rank1).isGreaterThan(xAt5.score());
  }

  @Test
  void hybridBeatsVectorOnlyForALexicallyExactMatch() {
    // Scenario: a query for a rare identifier "parseJwt".
    // Vector retriever ranks it low (semantically similar noise outranks it);
    // lexical retriever ranks the exact match #1. Fusion must lift it to the top,
    // demonstrating hybrid > vector-only.
    String relevant = "chunk-parseJwt";
    List<String> vectorOnly =
        List.of("noise-1", "noise-2", "noise-3", "noise-4", relevant); // relevant is 5th
    // The lexical retriever finds the exact identifier match at rank 1 (distinct noise).
    List<String> lexical = List.of(relevant, "noise-5");

    // Vector-only ranking: relevant is last (semantically buried under noise).
    List<Scored<String>> vectorRanking = rrf.fuse(vectorOnly);
    assertThat(vectorRanking.get(vectorRanking.size() - 1).key()).isEqualTo(relevant);

    // Hybrid ranking: the lexical exact-match lifts 'relevant' to #1.
    List<Scored<String>> hybrid = rrf.fuse(vectorOnly, lexical);
    assertThat(hybrid.get(0).key()).isEqualTo(relevant);
  }

  @Test
  void nullListsAreIgnored() {
    List<Scored<String>> fused = rrf.fuse(List.of("a"), null);
    assertThat(fused).extracting(Scored::key).containsExactly("a");
  }

  @Test
  void invalidKRejected() {
    assertThatThrownBy(() -> new ReciprocalRankFusion(0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
