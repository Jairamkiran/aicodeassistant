package com.jairam.aicodeassistant.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructuredOutputsTest {

  private final ObjectMapper mapper = new ObjectMapper();

  record Sample(String name, int count, List<String> tags) {}

  @Test
  void parsesPlainJsonObject() {
    Sample s =
        StructuredOutputs.parse(
            "{\"name\":\"x\",\"count\":2,\"tags\":[\"a\"]}", Sample.class, mapper);
    assertThat(s.name()).isEqualTo("x");
    assertThat(s.count()).isEqualTo(2);
    assertThat(s.tags()).containsExactly("a");
  }

  @Test
  void extractsJsonFromMarkdownFencesAndProse() {
    String reply =
        "Sure! Here is the review:\n```json\n{\"name\":\"y\",\"count\":0,\"tags\":[]}\n```\nHope that helps.";
    Sample s = StructuredOutputs.parse(reply, Sample.class, mapper);
    assertThat(s.name()).isEqualTo("y");
  }

  @Test
  void ignoresBracesInsideStringLiterals() {
    String reply = "{\"name\":\"a } weird { name\",\"count\":1,\"tags\":[]}";
    Sample s = StructuredOutputs.parse(reply, Sample.class, mapper);
    assertThat(s.name()).isEqualTo("a } weird { name");
  }

  @Test
  void extractsFirstBalancedArray() {
    String json = StructuredOutputs.extractJson("noise [1, 2, [3, 4]] trailing");
    assertThat(json).isEqualTo("[1, 2, [3, 4]]");
  }

  @Test
  void throwsWhenNoJsonPresent() {
    assertThatThrownBy(() -> StructuredOutputs.parse("no json here", Sample.class, mapper))
        .isInstanceOf(StructuredOutputException.class);
  }

  @Test
  void throwsOnUnbalancedJson() {
    assertThat(StructuredOutputs.extractJson("{\"a\": 1")).isNull();
  }

  @Test
  void jsonInstructionIncludesSchemaHint() {
    String instruction = StructuredOutputs.jsonInstruction("{\"x\": \"int\"}");
    assertThat(instruction).contains("SINGLE valid JSON").contains("{\"x\": \"int\"}");
  }
}
