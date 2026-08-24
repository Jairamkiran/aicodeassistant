package com.jairam.aicodeassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

/**
 * Entry point for the AI Software Engineering Assistant modular monolith (the {@code app}
 * deployable).
 *
 * <p>Placed at the base package {@code com.jairam.aicodeassistant} so Spring Modulith treats every
 * {@code com.jairam.aicodeassistant.<context>} sub-package as an application module and component
 * scanning discovers all bounded contexts.
 *
 * <p>The {@code platform} shared kernel is declared an OPEN module (see its {@code package-info}),
 * so every bounded context may reference its error model, events, and pagination types without
 * breaching module boundaries. See ADR-0002.
 *
 * <p>{@link Modulith} enumerates the modules that make up this deployable; it is the composition
 * root that aggregates the full system for the web/API surface.
 */
@Modulith(systemName = "AI Software Engineering Assistant")
@SpringBootApplication
public class AiCodeAssistantApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiCodeAssistantApplication.class, args);
  }
}
