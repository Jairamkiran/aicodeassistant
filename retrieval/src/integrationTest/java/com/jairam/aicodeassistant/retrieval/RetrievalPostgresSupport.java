package com.jairam.aicodeassistant.retrieval;

import com.jairam.aicodeassistant.ai.embedding.EmbeddingClient;
import com.jairam.aicodeassistant.retrieval.chunk.ChunkVectorStore;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for retrieval integration tests: a real pgvector Postgres (Testcontainers) with the minimal
 * code_chunks schema applied, a Spring context scanning only the retrieval module, and a
 * DETERMINISTIC stub {@link EmbeddingClient} (the IT exercises the search SQL + fusion, not real
 * embeddings — those are WireMock- tested in the ai module). Requires Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class RetrievalPostgresSupport {

  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("retrieval")
          .withUsername("retrieval")
          .withPassword("retrieval");

  static {
    POSTGRES.start();
  }

  @Autowired private ChunkVectorStore chunkVectorStore;
  @Autowired private CodeSearch codeSearch;
  @Autowired private DataSource dataSource;

  @BeforeAll
  void applySchema() throws Exception {
    try (var conn = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(conn, new ClassPathResource("retrieval-it-schema.sql"));
    }
  }

  ChunkVectorStore vectorStore() {
    return chunkVectorStore;
  }

  CodeSearch codeSearch() {
    return codeSearch;
  }

  /** Minimal bootable app + a stub embedding client for the retrieval IT. */
  @SpringBootApplication(scanBasePackages = "com.jairam.aicodeassistant.retrieval")
  static class ItConfig {

    @Bean
    DataSource dataSource() {
      return DataSourceBuilder.create()
          .url(POSTGRES.getJdbcUrl())
          .username(POSTGRES.getUsername())
          .password(POSTGRES.getPassword())
          .build();
    }

    /**
     * Deterministic fake embeddings: keyword-derived vectors so the vector retriever behaves
     * predictably in the IT without a real model. The search infrastructure (SQL, fusion,
     * provenance) is what's under test here.
     */
    @Bean
    @Primary
    EmbeddingClient stubEmbeddingClient() {
      return new EmbeddingClient() {
        @Override
        public int dimension() {
          return 768;
        }

        @Override
        public List<float[]> embedAll(List<String> texts) {
          List<float[]> out = new ArrayList<>();
          for (String text : texts) {
            float[] v = new float[768];
            // Cheap deterministic hashing into the first few dims.
            v[0] = text.toLowerCase().contains("parsejwt") ? 1f : 0.1f;
            v[1] = text.length() % 7;
            out.add(v);
          }
          return out;
        }
      };
    }
  }
}
