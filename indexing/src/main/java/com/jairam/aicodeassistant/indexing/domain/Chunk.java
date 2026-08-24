package com.jairam.aicodeassistant.indexing.domain;

/**
 * A text chunk of a source file, with its line span and detected language.
 *
 * @param filePath repo-relative file path
 * @param language language tag inferred from the extension, or null
 * @param startLine first line (1-based, inclusive)
 * @param endLine last line (inclusive)
 * @param content the chunk text
 */
public record Chunk(String filePath, String language, int startLine, int endLine, String content) {}
