package com.jairam.aicodeassistant.indexing.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Splits source files into overlapping, line-based text chunks suitable for embedding.
 *
 * <p>Design (see ADR-0010): text/line windowing — NOT language-aware AST parsing. Windows of {@code
 * linesPerChunk} lines with {@code overlapLines} of overlap so a construct spanning a boundary
 * still appears whole in one chunk. This is what most production RAG pipelines ship first;
 * AST-aware chunking is deferred until retrieval quality (M5) shows it is needed.
 *
 * <p>Files that are empty are skipped; binary detection and size limits are applied by the cloner
 * (which decides what counts as a "text file"), so the chunker assumes it receives text. Language
 * is tagged by file extension.
 */
public class Chunker {

  private static final Map<String, String> EXTENSION_LANGUAGE =
      Map.ofEntries(
          Map.entry("java", "java"),
          Map.entry("kt", "kotlin"),
          Map.entry("py", "python"),
          Map.entry("js", "javascript"),
          Map.entry("ts", "typescript"),
          Map.entry("tsx", "typescript"),
          Map.entry("go", "go"),
          Map.entry("rs", "rust"),
          Map.entry("rb", "ruby"),
          Map.entry("cs", "csharp"),
          Map.entry("cpp", "cpp"),
          Map.entry("c", "c"),
          Map.entry("h", "c"),
          Map.entry("sql", "sql"),
          Map.entry("sh", "shell"),
          Map.entry("md", "markdown"),
          Map.entry("yaml", "yaml"),
          Map.entry("yml", "yaml"),
          Map.entry("json", "json"),
          Map.entry("xml", "xml"));

  private final int linesPerChunk;
  private final int overlapLines;

  public Chunker(int linesPerChunk, int overlapLines) {
    if (linesPerChunk <= 0) {
      throw new IllegalArgumentException("linesPerChunk must be > 0");
    }
    if (overlapLines < 0 || overlapLines >= linesPerChunk) {
      throw new IllegalArgumentException("overlapLines must be in [0, linesPerChunk)");
    }
    this.linesPerChunk = linesPerChunk;
    this.overlapLines = overlapLines;
  }

  /** Reasonable defaults for code: 60-line windows with 10 lines of overlap. */
  public static Chunker withDefaults() {
    return new Chunker(60, 10);
  }

  /** Chunks a single file. Returns empty for blank content. */
  public List<Chunk> chunk(SourceFile file) {
    List<Chunk> chunks = new ArrayList<>();
    if (file.content() == null || file.content().isBlank()) {
      return chunks;
    }
    String language = languageOf(file.path());
    String[] lines = file.content().split("\n", -1);
    int total = lines.length;
    int step = linesPerChunk - overlapLines;

    for (int start = 0; start < total; start += step) {
      int end = Math.min(start + linesPerChunk, total);
      StringBuilder sb = new StringBuilder();
      for (int i = start; i < end; i++) {
        sb.append(lines[i]);
        if (i < end - 1) {
          sb.append('\n');
        }
      }
      String content = sb.toString();
      if (!content.isBlank()) {
        // Line numbers are 1-based and inclusive.
        chunks.add(new Chunk(file.path(), language, start + 1, end, content));
      }
      if (end == total) {
        break;
      }
    }
    return chunks;
  }

  /** Chunks a batch of files, flattening the result. */
  public List<Chunk> chunkAll(List<SourceFile> files) {
    List<Chunk> all = new ArrayList<>();
    for (SourceFile file : files) {
      all.addAll(chunk(file));
    }
    return all;
  }

  /** Infers a language tag from the file extension, or null if unknown. */
  static String languageOf(String path) {
    int dot = path.lastIndexOf('.');
    if (dot < 0 || dot == path.length() - 1) {
      return null;
    }
    return EXTENSION_LANGUAGE.get(path.substring(dot + 1).toLowerCase(java.util.Locale.ROOT));
  }
}
