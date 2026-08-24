package com.jairam.aicodeassistant.conversation.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * Serialises a turn's citations to/from a JSON string column. Kept as a TEXT column (not JSONB) so
 * the mapping is identical on Postgres and the H2 test DB. Uses a private static ObjectMapper —
 * converters are instantiated by the JPA provider, so we cannot inject one.
 */
@Converter
class CitationsJsonConverter
    implements AttributeConverter<List<ChatTurnEntity.CitationRow>, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<List<ChatTurnEntity.CitationRow>> TYPE =
      new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<ChatTurnEntity.CitationRow> attribute) {
    try {
      return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialise citations", e);
    }
  }

  @Override
  public List<ChatTurnEntity.CitationRow> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(dbData, TYPE);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialise citations", e);
    }
  }
}
