package com.jairam.aicodeassistant.retrieval.adapter.rest.dto;

import java.util.UUID;

/**
 * A search hit for the API, with file:line provenance so the UI can link straight to the source
 * location.
 */
public record SearchHitView(
    UUID chunkId,
    UUID repositoryId,
    String filePath,
    String language,
    int startLine,
    int endLine,
    String snippet,
    double score,
    String source) {}
