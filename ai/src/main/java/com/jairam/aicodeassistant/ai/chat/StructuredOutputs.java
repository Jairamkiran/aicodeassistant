package com.jairam.aicodeassistant.ai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

/**
 * Helpers for coaxing and parsing structured (JSON) answers out of a {@link ChatModel} without any
 * provider-specific "JSON mode" — the same code works against Ollama and OpenAI (ADR-0015).
 *
 * <p>Two responsibilities, both pure:
 *
 * <ul>
 *   <li>{@link #jsonInstruction} — a system-message fragment instructing the model to reply with a
 *       single JSON document and nothing else;
 *   <li>{@link #parse} — extract the first balanced JSON object/array from a model reply (models
 *       often wrap JSON in prose or ```json fences) and deserialize it.
 * </ul>
 *
 * <p>Kept in the {@code chat} named interface because callers in other modules (e.g. code review)
 * need it; it exposes no provider types.
 */
public final class StructuredOutputs {

  private StructuredOutputs() {}

  /**
   * A reusable instruction to append to a system prompt. {@code schemaHint} is a short,
   * human-readable description of the desired shape (field names + types); it is not a formal JSON
   * Schema, which real models follow poorly when small/local.
   */
  public static String jsonInstruction(String schemaHint) {
    Objects.requireNonNull(schemaHint, "schemaHint");
    return """
        Respond with a SINGLE valid JSON document and nothing else — no prose, no
        markdown fences, no comments. The JSON must match this shape:
        """
        + schemaHint;
  }

  /**
   * Extracts and parses the first balanced JSON value from a model reply.
   *
   * @param reply the raw model text (may contain fences or surrounding prose)
   * @param type the target type to deserialize into
   * @param mapper the shared Jackson mapper
   * @throws StructuredOutputException if no JSON is found or it cannot be parsed into {@code type}
   */
  public static <T> T parse(String reply, Class<T> type, ObjectMapper mapper) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(mapper, "mapper");
    String json = extractJson(reply);
    if (json == null) {
      throw new StructuredOutputException("no JSON value found in model reply");
    }
    try {
      return mapper.readValue(json, type);
    } catch (Exception e) {
      throw new StructuredOutputException("could not parse model JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Returns the first balanced {@code {...}} or {@code [...]} substring, respecting strings and
   * escapes so braces inside string literals do not confuse the matcher. Returns null if none.
   */
  static String extractJson(String reply) {
    if (reply == null) {
      return null;
    }
    int start = firstStructuralStart(reply);
    if (start < 0) {
      return null;
    }
    char open = reply.charAt(start);
    char close = open == '{' ? '}' : ']';
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = start; i < reply.length(); i++) {
      char c = reply.charAt(i);
      if (inString) {
        if (escaped) {
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == '"') {
          inString = false;
        }
        continue;
      }
      if (c == '"') {
        inString = true;
      } else if (c == open) {
        depth++;
      } else if (c == close) {
        depth--;
        if (depth == 0) {
          return reply.substring(start, i + 1);
        }
      }
    }
    return null; // unbalanced
  }

  private static int firstStructuralStart(String reply) {
    for (int i = 0; i < reply.length(); i++) {
      char c = reply.charAt(i);
      if (c == '{' || c == '[') {
        return i;
      }
    }
    return -1;
  }
}
