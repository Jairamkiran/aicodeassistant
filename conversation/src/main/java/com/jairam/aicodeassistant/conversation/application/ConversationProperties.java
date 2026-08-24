package com.jairam.aicodeassistant.conversation.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG chat tuning, bound from {@code aicodeassistant.conversation}.
 *
 * @param retrievalLimit how many chunks to retrieve as candidate context
 * @param memoryWindowTurns how many prior turns to include (windowed memory)
 * @param contextCharBudget max characters of retrieved context to pack (token proxy)
 */
@ConfigurationProperties(prefix = "aicodeassistant.conversation")
public record ConversationProperties(
    int retrievalLimit, int memoryWindowTurns, int contextCharBudget) {

  public ConversationProperties {
    if (retrievalLimit <= 0) {
      retrievalLimit = 8;
    }
    if (memoryWindowTurns <= 0) {
      memoryWindowTurns = 6;
    }
    if (contextCharBudget <= 0) {
      contextCharBudget = 12_000;
    }
  }
}
