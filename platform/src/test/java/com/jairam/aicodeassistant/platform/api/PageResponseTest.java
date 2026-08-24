package com.jairam.aicodeassistant.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {

  @Test
  void ofComputesTotalPages() {
    var page = PageResponse.of(List.of("a", "b"), 0, 2, 5);

    assertThat(page.totalPages()).isEqualTo(3); // ceil(5/2)
    assertThat(page.totalElements()).isEqualTo(5);
    assertThat(page.content()).containsExactly("a", "b");
  }

  @Test
  void ofWithZeroSizeYieldsZeroPages() {
    var page = PageResponse.of(List.of(), 0, 0, 0);
    assertThat(page.totalPages()).isZero();
  }

  @Test
  void mapPreservesPaginationMetadata() {
    var page = PageResponse.of(List.of(1, 2, 3), 1, 3, 10);

    PageResponse<String> mapped = page.map(Object::toString);

    assertThat(mapped.content()).containsExactly("1", "2", "3");
    assertThat(mapped.page()).isEqualTo(1);
    assertThat(mapped.size()).isEqualTo(3);
    assertThat(mapped.totalElements()).isEqualTo(10);
    assertThat(mapped.totalPages()).isEqualTo(page.totalPages());
  }

  @Test
  void contentIsDefensivelyCopiedAndImmutable() {
    var mutable = new java.util.ArrayList<>(List.of("x"));
    var page = new PageResponse<>(mutable, 0, 20, 1, 1);
    mutable.add("y"); // must not leak into the record

    assertThat(page.content()).containsExactly("x");
    assertThat(page.content()).isUnmodifiable();
  }

  @Test
  void nullContentBecomesEmptyList() {
    var page = new PageResponse<String>(null, 0, 20, 0, 0);
    assertThat(page.content()).isEmpty();
  }
}
