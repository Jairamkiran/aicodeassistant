/**
 * Code Intelligence bounded context.
 *
 * <p>Spring Modulith application module. Owns AI-assisted code review: it retrieves grounding code
 * ({@code retrieval :: search}), asks the model for a structured JSON review ({@code ai :: chat} +
 * {@code StructuredOutputs}), and maps it to domain findings. Organization authorization uses
 * {@code iam :: api}. These are the only cross-context dependencies (declared below,
 * modularity-verified); no provider, persistence, or JSON internals of other modules are touched.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Code Intelligence",
    allowedDependencies = {"retrieval :: search", "ai :: chat", "iam :: api", "platform"})
package com.jairam.aicodeassistant.codeintel;
