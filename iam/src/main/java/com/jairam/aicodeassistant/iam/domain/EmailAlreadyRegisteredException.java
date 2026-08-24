package com.jairam.aicodeassistant.iam.domain;

import com.jairam.aicodeassistant.platform.error.ConflictException;
import java.util.Map;

/** Raised when registering an email that already exists. Renders as HTTP 409. */
public class EmailAlreadyRegisteredException extends ConflictException {

  private static final long serialVersionUID = 1L;

  public EmailAlreadyRegisteredException(String email) {
    super("Email is already registered", Map.of("email", email));
  }
}
