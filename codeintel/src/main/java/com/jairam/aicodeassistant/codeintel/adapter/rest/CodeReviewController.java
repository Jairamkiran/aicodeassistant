package com.jairam.aicodeassistant.codeintel.adapter.rest;

import com.jairam.aicodeassistant.codeintel.adapter.rest.dto.ReviewRequest;
import com.jairam.aicodeassistant.codeintel.adapter.rest.dto.ReviewView;
import com.jairam.aicodeassistant.codeintel.application.CodeReviewService;
import com.jairam.aicodeassistant.codeintel.domain.CodeReview;
import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Code Intelligence endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/v1/code-reviews} — run a structured AI code review over a repository for a
 *       focus topic; requires read access to the organization.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/code-reviews")
class CodeReviewController {

  private final CodeReviewService reviewService;
  private final OrganizationAccess organizationAccess;

  CodeReviewController(CodeReviewService reviewService, OrganizationAccess organizationAccess) {
    this.reviewService = reviewService;
    this.organizationAccess = organizationAccess;
  }

  @PostMapping
  ReviewView review(Authentication authentication, @Valid @RequestBody ReviewRequest request) {
    UUID userId = currentUserId(authentication);
    if (!organizationAccess.canRead(userId, request.organizationId())) {
      throw new NotAuthorizedException(request.organizationId());
    }
    CodeReview review =
        reviewService.review(request.organizationId(), request.repositoryId(), request.focus());
    return ReviewView.from(review);
  }

  private static UUID currentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotAuthenticatedException();
    }
    return UUID.fromString(authentication.getName());
  }

  /** Caller cannot read the organization. HTTP 403. */
  static final class NotAuthorizedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthorizedException(UUID organizationId) {
      super(
          ErrorType.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          "You cannot access this organization",
          Map.of("organizationId", organizationId.toString()));
    }
  }

  /** No authenticated principal. HTTP 401. */
  static final class NotAuthenticatedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthenticatedException() {
      super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Not authenticated", Map.of());
    }
  }
}
