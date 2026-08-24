package com.jairam.aicodeassistant.indexing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ChunkerTest {

  @Test
  void singleSmallFileIsOneChunk() {
    var chunker = new Chunker(60, 10);
    List<Chunk> chunks = chunker.chunk(new SourceFile("A.java", "a\nb\nc"));

    assertThat(chunks).hasSize(1);
    assertThat(chunks.get(0).startLine()).isEqualTo(1);
    assertThat(chunks.get(0).endLine()).isEqualTo(3);
    assertThat(chunks.get(0).language()).isEqualTo("java");
  }

  @Test
  void largeFileIsWindowedWithOverlap() {
    var chunker = new Chunker(10, 3); // step = 7
    String content =
        IntStream.rangeClosed(1, 20).mapToObj(Integer::toString).collect(Collectors.joining("\n"));

    List<Chunk> chunks = chunker.chunk(new SourceFile("big.txt", content));

    // Windows start at lines 1, 8, 15 (1-based) → 3 chunks.
    assertThat(chunks).hasSize(3);
    assertThat(chunks.get(0).startLine()).isEqualTo(1);
    assertThat(chunks.get(0).endLine()).isEqualTo(10);
    assertThat(chunks.get(1).startLine()).isEqualTo(8); // overlap of 3 with previous
    assertThat(chunks.get(2).endLine()).isEqualTo(20);
  }

  @Test
  void blankFileProducesNoChunks() {
    assertThat(Chunker.withDefaults().chunk(new SourceFile("x.txt", "   "))).isEmpty();
    assertThat(Chunker.withDefaults().chunk(new SourceFile("x.txt", ""))).isEmpty();
  }

  @Test
  void languageInferredFromExtension() {
    assertThat(Chunker.languageOf("src/Main.java")).isEqualTo("java");
    assertThat(Chunker.languageOf("app.py")).isEqualTo("python");
    assertThat(Chunker.languageOf("README")).isNull();
    assertThat(Chunker.languageOf("weird.xyz")).isNull();
  }

  @Test
  void invalidWindowConfigRejected() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new Chunker(0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new Chunker(10, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
