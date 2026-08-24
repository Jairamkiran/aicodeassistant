package com.jairam.aicodeassistant.codeintel.adapter.rest.dto;

import com.jairam.aicodeassistant.codeintel.domain.CodeReview;
import java.util.List;
import java.util.UUID;

/** API representation of a completed code review. */
public record ReviewView(
    UUID repositoryId, String focus, String summary, List<FindingView> findings) {

  /** API representation of a single finding. */
  public record FindingView(
      String severity,
      String category,
      String filePath,
      int startLine,
      int endLine,
      String title,
      String detail,
      String recommendation) {}

  /** Maps a domain {@link CodeReview} to its API view. */
  public static ReviewView from(CodeReview review) {
    List<FindingView> findings =
        review.findings().stream()
            .map(
                f ->
                    new FindingView(
                        f.severity().name(),
                        f.category(),
                        f.filePath(),
                        f.startLine(),
                        f.endLine(),
                        f.title(),
                        f.detail(),
                        f.recommendation()))
            .toList();
    return new ReviewView(review.repositoryId(), review.focus(), review.summary(), findings);
  }
}
