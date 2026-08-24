package com.jairam.aicodeassistant.ai.chat;

/**
 * Token accounting for a chat completion, as reported by the provider. Captured as metrics ({@code
 * ai.chat.tokens}) for observability and future cost attribution — this is NOT a billing ledger
 * (that is M13).
 *
 * @param promptTokens tokens in the prompt (input)
 * @param completionTokens tokens generated (output)
 */
public record TokenUsage(int promptTokens, int completionTokens) {

  public static final TokenUsage UNKNOWN = new TokenUsage(0, 0);

  public int totalTokens() {
    return promptTokens + completionTokens;
  }
}
