package com.jairam.aicodeassistant.codeintel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.ai.chat.ChatModel;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.ChatToken;
import com.jairam.aicodeassistant.ai.chat.StructuredOutputException;
import com.jairam.aicodeassistant.ai.chat.TokenUsage;
import com.jairam.aicodeassistant.codeintel.domain.CodeReview;
import com.jairam.aicodeassistant.codeintel.domain.ReviewSeverity;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CodeReviewService} using in-test fakes (no Spring, no I/O). */
class CodeReviewServiceTest {

  private static final UUID ORG = UUID.randomUUID();
  private static final UUID REPO = UUID.randomUUID();
  private final ObjectMapper mapper = new ObjectMapper();
  private final CodeIntelProperties properties = new CodeIntelProperties(0, 0);

  @Test
  void parsesStructuredFindingsAndOrdersBySeverity() {
    String json =
        """
        Here is my review:
        {
          "summary": "Two issues found.",
          "findings": [
            {"severity":"LOW","category":"style","filePath":"A.java","startLine":3,"endLine":3,
             "title":"Naming","detail":"d","recommendation":"r"},
            {"severity":"CRITICAL","category":"security","filePath":"B.java","startLine":10,"endLine":12,
             "title":"SQLi","detail":"d","recommendation":"r"}
          ]
        }
        """;
    CodeReviewService service =
        new CodeReviewService(fixedSearch(oneHit()), fixedModel(json), mapper, properties);

    CodeReview review = service.review(ORG, REPO, "security");

    assertThat(review.summary()).isEqualTo("Two issues found.");
    assertThat(review.findings()).hasSize(2);
    // CRITICAL must come before LOW.
    assertThat(review.findings().get(0).severity()).isEqualTo(ReviewSeverity.CRITICAL);
    assertThat(review.findings().get(0).filePath()).isEqualTo("B.java");
    assertThat(review.findings().get(1).severity()).isEqualTo(ReviewSeverity.LOW);
  }

  @Test
  void toleratesEmptyFindings() {
    String json = "{\"summary\":\"Looks good.\",\"findings\":[]}";
    CodeReviewService service =
        new CodeReviewService(fixedSearch(oneHit()), fixedModel(json), mapper, properties);

    CodeReview review = service.review(ORG, REPO, "anything");

    assertThat(review.findings()).isEmpty();
    assertThat(review.summary()).isEqualTo("Looks good.");
  }

  @Test
  void unknownSeverityFallsBackToInfo() {
    String json =
        "{\"summary\":\"x\",\"findings\":[{\"severity\":\"BOGUS\",\"filePath\":\"A\",\"title\":\"t\"}]}";
    CodeReviewService service =
        new CodeReviewService(fixedSearch(oneHit()), fixedModel(json), mapper, properties);

    CodeReview review = service.review(ORG, REPO, "x");

    assertThat(review.findings().get(0).severity()).isEqualTo(ReviewSeverity.INFO);
  }

  @Test
  void throwsWhenModelReturnsNoJson() {
    CodeReviewService service =
        new CodeReviewService(
            fixedSearch(oneHit()), fixedModel("I could not review this."), mapper, properties);

    assertThatThrownBy(() -> service.review(ORG, REPO, "x"))
        .isInstanceOf(StructuredOutputException.class);
  }

  @Test
  void passesRetrievalLimitAndScopeToSearch() {
    RecordingSearch search = new RecordingSearch(oneHit());
    CodeReviewService service =
        new CodeReviewService(
            search, fixedModel("{\"summary\":\"\",\"findings\":[]}"), mapper, properties);

    service.review(ORG, REPO, "focus text");

    assertThat(search.lastQuery.organizationId()).isEqualTo(ORG);
    assertThat(search.lastQuery.repositoryId()).isEqualTo(REPO);
    assertThat(search.lastQuery.text()).isEqualTo("focus text");
    assertThat(search.lastQuery.limit()).isEqualTo(properties.reviewRetrievalLimit());
  }

  // --- fakes -------------------------------------------------------------------

  private static List<SearchResult> oneHit() {
    return List.of(
        new SearchResult(
            UUID.randomUUID(),
            REPO,
            "A.java",
            "java",
            1,
            5,
            "class A {}",
            1.0,
            SearchResult.Source.HYBRID));
  }

  private static CodeSearch fixedSearch(List<SearchResult> results) {
    return query -> results;
  }

  private static ChatModel fixedModel(String content) {
    return new ChatModel() {
      @Override
      public ChatResponse chat(ChatRequest request) {
        return new ChatResponse(content, TokenUsage.UNKNOWN, "stop");
      }

      @Override
      public Stream<ChatToken> chatStream(ChatRequest request) {
        return Stream.of(ChatToken.delta(content), ChatToken.done(TokenUsage.UNKNOWN));
      }

      @Override
      public String provider() {
        return "fake";
      }
    };
  }

  private static final class RecordingSearch implements CodeSearch {
    private final List<SearchResult> results;
    private SearchQuery lastQuery;

    RecordingSearch(List<SearchResult> results) {
      this.results = results;
    }

    @Override
    public List<SearchResult> search(SearchQuery query) {
      this.lastQuery = query;
      return results;
    }
  }
}
