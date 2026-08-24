package com.jairam.aicodeassistant.ai.chat;

/**
 * One streamed increment of a chat response.
 *
 * @param delta the text fragment for this step (may be empty on the final token)
 * @param done true for the terminal token; carries the usage if the provider reports it at stream
 *     end
 * @param usage token accounting on the terminal token (UNKNOWN otherwise)
 */
public record ChatToken(String delta, boolean done, TokenUsage usage) {

  public static ChatToken delta(String delta) {
    return new ChatToken(delta, false, TokenUsage.UNKNOWN);
  }

  public static ChatToken done(TokenUsage usage) {
    return new ChatToken("", true, usage == null ? TokenUsage.UNKNOWN : usage);
  }
}
