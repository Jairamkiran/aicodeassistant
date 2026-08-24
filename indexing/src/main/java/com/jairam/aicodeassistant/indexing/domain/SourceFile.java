package com.jairam.aicodeassistant.indexing.domain;

/**
 * A text file read from a cloned repository.
 *
 * @param path repo-relative path (forward slashes)
 * @param content full UTF-8 text content
 */
public record SourceFile(String path, String content) {}
