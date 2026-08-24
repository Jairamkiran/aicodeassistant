/**
 * Public chunk-store API — a Spring Modulith {@link org.springframework.modulith.NamedInterface
 * named interface}.
 *
 * <p>{@code CodeChunk} + {@code ChunkVectorStore} are the surface the indexing module uses to
 * persist embedded chunks. The pgvector adapter is in {@code chunk.internal}.
 */
@org.springframework.modulith.NamedInterface("chunk")
package com.jairam.aicodeassistant.retrieval.chunk;
