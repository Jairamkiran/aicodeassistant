package com.jairam.aicodeassistant.codeintel.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Code-intelligence tuning, bound from {@code aicodeassistant.codeintel}.
 *
 * @param reviewRetrievalLimit how many chunks to retrieve as review context
 * @param contextCharBudget max characters of retrieved code to pack into the review prompt
 */
@ConfigurationProperties(prefix = "aicodeassistant.codeintel")
public record CodeIntelProperties(int reviewRetrievalLimit, int contextCharBudget) {

  public CodeIntelProperties {
    if (reviewRetrievalLimit <= 0) {
      reviewRetrievalLimit = 12;
    }
    if (contextCharBudget <= 0) {
      contextCharBudget = 14_000;
    }
  }
}
