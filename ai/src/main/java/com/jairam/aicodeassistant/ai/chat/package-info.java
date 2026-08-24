/**
 * Public chat API — a Spring Modulith {@link org.springframework.modulith.NamedInterface named
 * interface}.
 *
 * <p>{@code ChatModel} + its request/response/message/token records are the only chat surface other
 * modules use. The Ollama and OpenAI HTTP clients and their JSON DTOs live in {@code
 * chat.internal}; exactly one is active per config.
 */
@org.springframework.modulith.NamedInterface("chat")
package com.jairam.aicodeassistant.ai.chat;
