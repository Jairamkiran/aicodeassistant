package com.jairam.aicodeassistant.ai.embedding;

import java.util.List;

/**
 * Public API for producing text embeddings — the only embedding surface other modules use. The
 * concrete provider (Ollama in M4) and its HTTP/JSON specifics are encapsulated in the adapter.
 *
 * <p>Deliberately NOT a provider-abstraction port with multiple implementations: Ollama is the only
 * provider today. When a second provider is added (M6, OpenAI) the seam is already an interface,
 * but we do not speculate on provider-selection config now.
 */
public interface EmbeddingClient {

  /** The embedding dimension this client produces (e.g. 768 for nomic-embed-text). */
  int dimension();

  /**
   * Embeds a batch of texts, preserving order (result[i] embeds input[i]).
   *
   * @throws EmbeddingException if the provider is unavailable after retries
   */
  List<float[]> embedAll(List<String> texts);

  /** Convenience for a single text. */
  default float[] embed(String text) {
    return embedAll(List.of(text)).get(0);
  }
}
