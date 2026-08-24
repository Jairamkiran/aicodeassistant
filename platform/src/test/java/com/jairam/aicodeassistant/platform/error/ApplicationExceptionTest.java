package com.jairam.aicodeassistant.platform.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApplicationExceptionTest {

  @Test
  void resourceNotFoundCarriesCategoryStatusAndProperties() {
    var ex = new ResourceNotFoundException("User", "abc-123");

    assertThat(ex.errorType()).isEqualTo(ErrorType.NOT_FOUND);
    assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getMessage()).contains("User").contains("abc-123");
    assertThat(ex.properties())
        .containsEntry("resource", "User")
        .containsEntry("identifier", "abc-123");
  }

  @Test
  void conflictExceptionDefaultsToConflictStatus() {
    var ex = new ConflictException("email already registered");

    assertThat(ex.errorType()).isEqualTo(ErrorType.CONFLICT);
    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex.properties()).isEmpty();
  }

  @Test
  void validationExceptionExposesStructuredProperties() {
    var ex = new ValidationException("bad range", Map.of("field", "size", "max", 100));

    assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.properties()).containsEntry("field", "size").containsEntry("max", 100);
  }

  @Test
  void propertiesAreImmutable() {
    var ex = new ConflictException("x", Map.of("k", "v"));
    assertThat(ex.properties()).isUnmodifiable();
  }

  @Test
  void errorTypeUrnIsStableAndAbsolute() {
    assertThat(ErrorType.NOT_FOUND.type())
        .isEqualTo("https://aicodeassistant.dev/problems/resource-not-found");
    assertThat(ErrorType.VALIDATION.code()).isEqualTo("validation-error");
  }
}
