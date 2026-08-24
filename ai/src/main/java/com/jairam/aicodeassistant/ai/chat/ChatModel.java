package com.jairam.aicodeassistant.ai.chat;

import java.util.stream.Stream;

/**
 * Provider-agnostic chat completion API — the public {@code chat} surface other modules (M7 RAG)
 * use. Exactly one implementation is active, selected by configuration ({@code
 * aicodeassistant.ai.chat.provider}: {@code ollama} default, or {@code openai}). This is the point
 * in the system where a provider abstraction is genuinely warranted: two real implementations
 * behind one port (see ADR-0012).
 */
public interface ChatModel {

  /** Blocking completion: returns the full reply and token usage. */
  ChatResponse chat(ChatRequest request);

  /**
   * Streaming completion: a lazy {@link Stream} of {@link ChatToken} deltas ending with a {@code
   * done} token. The stream holds an open HTTP response, so callers MUST close it
   * (try-with-resources) — do not leave it dangling.
   */
  Stream<ChatToken> chatStream(ChatRequest request);

  /** The active provider id (for logging/metrics/diagnostics). */
  String provider();
}
