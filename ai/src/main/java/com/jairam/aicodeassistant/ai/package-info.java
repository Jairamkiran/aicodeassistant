/**
 * AI Orchestration bounded context.
 *
 * <p>Spring Modulith application module owning AI provider integrations. In M4 it provides text
 * embeddings (Ollama). Provider HTTP clients and JSON DTOs live in {@code <feature>.internal};
 * other modules use only the public feature APIs (e.g. the {@code embedding} named interface).
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI Orchestration")
package com.jairam.aicodeassistant.ai;
