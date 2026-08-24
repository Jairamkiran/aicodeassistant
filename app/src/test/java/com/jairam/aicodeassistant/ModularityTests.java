package com.jairam.aicodeassistant;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies and documents the modular architecture.
 *
 * <p>{@link ApplicationModules#verify()} fails the build if any bounded context reaches into
 * another module's {@code internal} packages, if there are cyclic dependencies between modules, or
 * if a module depends on one it has not declared. This is the enforcement that makes our DDD
 * boundaries real rather than conventional — it runs in normal unit-test scope (no Docker needed).
 *
 * <p>{@code writeDocumentation()} additionally emits PlantUML C4 component diagrams + module
 * canvases under {@code build/spring-modulith-docs}, which we publish as living architecture
 * documentation.
 */
class ModularityTests {

  private final ApplicationModules modules =
      ApplicationModules.of(AiCodeAssistantApplication.class);

  @Test
  void verifiesModuleBoundaries() {
    modules.verify();
  }

  @Test
  void writesArchitectureDocumentation() {
    new Documenter(modules)
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml()
        .writeModuleCanvases();
  }
}
