package com.jairam.aicodeassistant.conversation.domain;

import java.util.UUID;

/**
 * A source citation attached to an assistant answer, pointing at the code chunk that grounded (part
 * of) the response.
 *
 * <p>Citations are derived from the chunks retrieved for the prompt — NOT parsed out of the model's
 * text (see ADR-0014). Provenance is therefore trustworthy: these are exactly the sources the model
 * was shown.
 *
 * @param index the 1-based reference number shown in the prompt (e.g. {@code [1]})
 * @param chunkId the retrieved chunk's id
 * @param repositoryId owning repository
 * @param filePath file path within the repo
 * @param startLine first line (1-based, inclusive)
 * @param endLine last line (inclusive)
 */
public record Citation(
    int index, UUID chunkId, UUID repositoryId, String filePath, int startLine, int endLine) {}
