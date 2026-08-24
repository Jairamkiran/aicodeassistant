/**
 * Public code-search API — a Spring Modulith {@link org.springframework.modulith.NamedInterface
 * named interface}.
 *
 * <p>{@code CodeSearch} + {@code SearchQuery} + {@code SearchResult} are the surface other modules
 * (the M7 chat feature) use to retrieve relevant chunks. The hybrid vector+lexical implementation,
 * RRF fusion, and SQL live in {@code search.internal}.
 */
@org.springframework.modulith.NamedInterface("search")
package com.jairam.aicodeassistant.retrieval.search;
