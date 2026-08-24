package com.jairam.aicodeassistant.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChatDomainTest {

  @Test
  void messageFactoriesSetRole() {
    assertThat(ChatMessage.system("s").role()).isEqualTo(ChatMessage.Role.SYSTEM);
    assertThat(ChatMessage.user("u").role()).isEqualTo(ChatMessage.Role.USER);
    assertThat(ChatMessage.assistant("a").role()).isEqualTo(ChatMessage.Role.ASSISTANT);
  }

  @Test
  void requestRejectsEmptyMessages() {
    assertThatThrownBy(() -> ChatRequest.of(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void requestDefaultsAreNull() {
    ChatRequest r = ChatRequest.of(List.of(ChatMessage.user("hi")));
    assertThat(r.model()).isNull();
    assertThat(r.temperature()).isNull();
    assertThat(r.maxTokens()).isNull();
  }

  @Test
  void tokenUsageTotals() {
    assertThat(new TokenUsage(3, 4).totalTokens()).isEqualTo(7);
  }

  @Test
  void chatTokenFactories() {
    assertThat(ChatToken.delta("x").done()).isFalse();
    assertThat(ChatToken.done(new TokenUsage(1, 1)).done()).isTrue();
  }
}
