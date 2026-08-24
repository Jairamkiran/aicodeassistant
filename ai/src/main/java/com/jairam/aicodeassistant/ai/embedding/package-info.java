/**
 * Public embedding API — a Spring Modulith {@link org.springframework.modulith.NamedInterface named
 * interface}.
 *
 * <p>{@code EmbeddingClient} + {@code EmbeddingException} are the only embedding surface other
 * modules may use. The Ollama HTTP client and its JSON DTOs live in {@code embedding.internal} and
 * never cross the boundary.
 */
@org.springframework.modulith.NamedInterface("embedding")
package com.jairam.aicodeassistant.ai.embedding;
