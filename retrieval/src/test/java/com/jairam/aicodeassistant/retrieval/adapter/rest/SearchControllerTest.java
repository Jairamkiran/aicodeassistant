package com.jairam.aicodeassistant.retrieval.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.retrieval.adapter.rest.dto.SearchHitView;
import com.jairam.aicodeassistant.retrieval.adapter.rest.dto.SearchRequest;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Plain unit test of the search controller's logic — authorization gate and result mapping —
 * invoked directly with mocked collaborators. No web/security machinery needed (kept simple per the
 * review directive); the HTTP + real search path is covered by the Testcontainers IT.
 */
class SearchControllerTest {

  private final CodeSearch codeSearch = mock(CodeSearch.class);
  private final OrganizationAccess organizationAccess = mock(OrganizationAccess.class);
  private final SearchController controller = new SearchController(codeSearch, organizationAccess);

  private static final UUID USER = UUID.randomUUID();
  private static final UUID ORG = UUID.randomUUID();

  private static Authentication auth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", List.of());
  }

  @Test
  void returnsMappedHitsWhenAuthorized() {
    when(organizationAccess.canRead(USER, ORG)).thenReturn(true);
    when(codeSearch.search(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            List.of(
                new SearchResult(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "src/Auth.java",
                    "java",
                    10,
                    20,
                    "parseJwt(token)",
                    0.42,
                    SearchResult.Source.HYBRID)));

    List<SearchHitView> hits =
        controller.search(auth(USER), new SearchRequest(ORG, null, null, "parseJwt", 5));

    assertThat(hits).hasSize(1);
    assertThat(hits.get(0).filePath()).isEqualTo("src/Auth.java");
    assertThat(hits.get(0).startLine()).isEqualTo(10);
    assertThat(hits.get(0).source()).isEqualTo("HYBRID");
  }

  @Test
  void forbiddenWhenCallerCannotReadOrg() {
    when(organizationAccess.canRead(USER, ORG)).thenReturn(false);

    assertThatThrownBy(
            () -> controller.search(auth(USER), new SearchRequest(ORG, null, null, "x", 5)))
        .isInstanceOf(SearchController.NotAuthorizedException.class);
  }

  @Test
  void snippetIsTruncatedForLongContent() {
    when(organizationAccess.canRead(USER, ORG)).thenReturn(true);
    String longContent = "x".repeat(1000);
    when(codeSearch.search(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            List.of(
                new SearchResult(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "f",
                    "java",
                    1,
                    50,
                    longContent,
                    0.1,
                    SearchResult.Source.VECTOR)));

    List<SearchHitView> hits =
        controller.search(auth(USER), new SearchRequest(ORG, null, null, "q", 5));

    assertThat(hits.get(0).snippet().length()).isLessThanOrEqualTo(400);
  }
}
