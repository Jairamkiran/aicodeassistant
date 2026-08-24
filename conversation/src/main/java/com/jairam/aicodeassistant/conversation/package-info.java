/**
 * Conversation bounded context — the flagship Repository Code Chat (RAG).
 *
 * <p>Owns chat sessions/turns and the RAG orchestrator: it retrieves grounding chunks ({@code
 * retrieval :: search}), assembles a token-budgeted, guardrail- fenced prompt, streams the answer
 * ({@code ai :: chat}), and returns citations to file:line. Org/session authorization uses {@code
 * iam :: api}. These are the only cross-context dependencies (declared below, modularity-verified);
 * no provider or persistence internals of other modules are touched.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Conversation",
    allowedDependencies = {"retrieval :: search", "ai :: chat", "iam :: api", "platform"})
package com.jairam.aicodeassistant.conversation;
