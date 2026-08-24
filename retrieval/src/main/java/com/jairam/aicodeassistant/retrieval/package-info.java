/**
 * Retrieval bounded context.
 *
 * <p>Spring Modulith application module owning the code-chunk vector store and hybrid search. Write
 * side (M4): the {@code chunk} named interface. Query side (M5): the {@code search} named interface
 * (pgvector cosine + Postgres FTS fused with reciprocal-rank fusion). Depends on {@code ai ::
 * embedding} (embed the query) and {@code iam :: api} (org-scoped authorization); adapters are
 * internal.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Retrieval",
    allowedDependencies = {"ai :: embedding", "iam :: api", "platform"})
package com.jairam.aicodeassistant.retrieval;
