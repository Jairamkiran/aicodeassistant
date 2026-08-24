package com.jairam.aicodeassistant.retrieval.adapter.rest;

import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import com.jairam.aicodeassistant.retrieval.adapter.rest.dto.SearchHitView;
import com.jairam.aicodeassistant.retrieval.adapter.rest.dto.SearchRequest;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hybrid code-search endpoint.
 *
 * <p>{@code POST /api/v1/search} runs the vector+lexical hybrid search, scoped to the caller's
 * organization (they must be able to read it — enforced via the iam {@code OrganizationAccess}
 * port). Returns hits with file:line provenance.
 */
@RestController
@RequestMapping("/api/v1/search")
class SearchController {

  private static final int SNIPPET_MAX_CHARS = 400;

  private final CodeSearch codeSearch;
  private final OrganizationAccess organizationAccess;

  SearchController(CodeSearch codeSearch, OrganizationAccess organizationAccess) {
    this.codeSearch = codeSearch;
    this.organizationAccess = organizationAccess;
  }

  @PostMapping
  List<SearchHitView> search(
      Authentication authentication, @Valid @RequestBody SearchRequest request) {
    UUID userId = currentUserId(authentication);
    if (!organizationAccess.canRead(userId, request.organizationId())) {
      throw new NotAuthorizedException(request.organizationId());
    }

    SearchQuery query =
        new SearchQuery(
            request.organizationId(),
            request.repositoryId(),
            request.language(),
            request.query(),
            request.effectiveLimit());

    return codeSearch.search(query).stream().map(SearchController::toView).toList();
  }

  private static SearchHitView toView(SearchResult r) {
    String content = r.content() == null ? "" : r.content();
    String snippet =
        content.length() > SNIPPET_MAX_CHARS ? content.substring(0, SNIPPET_MAX_CHARS) : content;
    return new SearchHitView(
        r.chunkId(),
        r.repositoryId(),
        r.filePath(),
        r.language(),
        r.startLine(),
        r.endLine(),
        snippet,
        r.score(),
        r.source().name());
  }

  private static UUID currentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotAuthenticatedException();
    }
    return UUID.fromString(authentication.getName());
  }

  /** Raised when the caller cannot read the target organization. HTTP 403. */
  static final class NotAuthorizedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthorizedException(UUID organizationId) {
      super(
          ErrorType.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          "You cannot search this organization",
          Map.of("organizationId", organizationId.toString()));
    }
  }

  /** Raised when there is no authenticated principal. HTTP 401. */
  static final class NotAuthenticatedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthenticatedException() {
      super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Not authenticated", Map.of());
    }
  }
}
