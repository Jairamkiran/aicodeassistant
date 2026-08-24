package com.jairam.aicodeassistant.codeintel.domain;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The result of an AI code review over a repository for a focus topic: an ordered list of {@link
 * ReviewFinding}s plus a short summary. Findings are ordered by descending severity.
 *
 * @param repositoryId the reviewed repository
 * @param focus the natural-language focus of the review (what the user asked to review)
 * @param summary a short overall summary
 * @param findings the findings, most severe first
 */
public record CodeReview(
    UUID repositoryId, String focus, String summary, List<ReviewFinding> findings) {

  public CodeReview {
    findings = findings == null ? List.of() : List.copyOf(findings);
  }

  /** Builds a review with findings sorted most-severe first (stable within a severity). */
  public static CodeReview of(
      UUID repositoryId, String focus, String summary, List<ReviewFinding> findings) {
    List<ReviewFinding> ordered =
        findings.stream().sorted(Comparator.comparingInt(f -> f.severity().ordinal())).toList();
    return new CodeReview(repositoryId, focus, summary == null ? "" : summary, ordered);
  }
}
