package com.jairam.aicodeassistant.codeintel.domain;

import java.util.Objects;

/**
 * A single, structured code-review finding grounded in a specific file/line span. Produced by the
 * AI reviewer from retrieved code; provenance ({@code filePath}, {@code startLine}) ties the
 * finding back to real code so the UI can open it.
 *
 * @param severity how serious the finding is
 * @param category short category label (e.g. {@code correctness}, {@code security})
 * @param filePath the file the finding refers to
 * @param startLine 1-based line where the issue begins (0 if unknown)
 * @param endLine 1-based line where the issue ends (0 if unknown)
 * @param title one-line summary
 * @param detail the explanation of the issue
 * @param recommendation the suggested fix
 */
public record ReviewFinding(
    ReviewSeverity severity,
    String category,
    String filePath,
    int startLine,
    int endLine,
    String title,
    String detail,
    String recommendation) {

  public ReviewFinding {
    Objects.requireNonNull(severity, "severity");
    category = category == null ? "general" : category;
    filePath = filePath == null ? "" : filePath;
    title = title == null ? "" : title;
    detail = detail == null ? "" : detail;
    recommendation = recommendation == null ? "" : recommendation;
  }
}
