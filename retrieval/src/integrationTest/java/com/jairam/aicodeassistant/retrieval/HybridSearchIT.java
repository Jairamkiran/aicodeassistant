package com.jairam.aicodeassistant.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.retrieval.chunk.ChunkVectorStore;
import com.jairam.aicodeassistant.retrieval.chunk.CodeChunk;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * End-to-end hybrid search against REAL Postgres + pgvector + FTS (Testcontainers). Upserts
 * embedded chunks via the M4 write store, then searches — asserting file:line provenance is
 * returned and that a lexically-exact identifier match is retrieved. Requires Docker; runs under
 * {@code ./gradlew integrationTest} in CI.
 *
 * <p>Extends the retrieval Testcontainers base (Postgres with pgvector + the V1/V8/V9 schema
 * applied) — see {@code RetrievalPostgresSupport}.
 */
class HybridSearchIT extends RetrievalPostgresSupport {

  private static final UUID ORG = UUID.randomUUID();
  private static final UUID REPO = UUID.randomUUID();

  private static float[] vec(float a, float b, float c) {
    // 768-dim vector with the first three components set (rest zero) — enough for
    // deterministic cosine ordering in the test.
    float[] v = new float[768];
    v[0] = a;
    v[1] = b;
    v[2] = c;
    return v;
  }

  @Test
  void upsertsThenFindsChunkWithProvenance() {
    ChunkVectorStore store = vectorStore();
    CodeSearch search = codeSearch();

    store.deleteByRepository(REPO);
    store.upsertAll(
        List.of(
            new CodeChunk(
                REPO,
                ORG,
                "src/Auth.java",
                "java",
                10,
                20,
                "public String parseJwt(String token) { return decode(token); }",
                vec(1, 0, 0)),
            new CodeChunk(
                REPO,
                ORG,
                "src/Util.java",
                "java",
                1,
                5,
                "int add(int a, int b){return a+b;}",
                vec(0, 1, 0))));

    // Lexically-exact query for the identifier.
    List<SearchResult> results = search.search(new SearchQuery(ORG, REPO, "parseJwt token", 5));

    assertThat(results).isNotEmpty();
    SearchResult top = results.get(0);
    assertThat(top.filePath()).isEqualTo("src/Auth.java");
    assertThat(top.startLine()).isEqualTo(10);
    assertThat(top.endLine()).isEqualTo(20);
    assertThat(top.content()).contains("parseJwt");
  }

  @Test
  void searchIsOrganizationScoped() {
    ChunkVectorStore store = vectorStore();
    CodeSearch search = codeSearch();
    UUID otherOrg = UUID.randomUUID();
    UUID otherRepo = UUID.randomUUID();

    store.upsertAll(
        List.of(
            new CodeChunk(
                otherRepo,
                otherOrg,
                "secret/Other.java",
                "java",
                1,
                3,
                "class Secret {}",
                vec(1, 1, 1))));

    // Searching ORG must not see the other org's chunk.
    List<SearchResult> results = search.search(new SearchQuery(ORG, null, "Secret", 5));
    assertThat(results).noneMatch(r -> r.repositoryId().equals(otherRepo));
  }
}
