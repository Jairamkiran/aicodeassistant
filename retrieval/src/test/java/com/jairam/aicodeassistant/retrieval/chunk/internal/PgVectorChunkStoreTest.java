package com.jairam.aicodeassistant.retrieval.chunk.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit test for the pgvector literal formatting — the one bit of non-trivial logic in the store
 * that is worth verifying without a database. (The actual upsert against pgvector is covered by a
 * Testcontainers IT.)
 */
class PgVectorChunkStoreTest {

  @Test
  void formatsFloatArrayAsPgvectorLiteral() {
    String literal = PgVectorChunkStore.toVectorLiteral(new float[] {0.1f, 0.2f, 0.3f});
    assertThat(literal).isEqualTo("[0.1,0.2,0.3]");
  }

  @Test
  void singleElementAndEmpty() {
    assertThat(PgVectorChunkStore.toVectorLiteral(new float[] {1.5f})).isEqualTo("[1.5]");
    assertThat(PgVectorChunkStore.toVectorLiteral(new float[] {})).isEqualTo("[]");
  }
}
