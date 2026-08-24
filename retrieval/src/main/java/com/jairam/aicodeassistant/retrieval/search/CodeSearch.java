package com.jairam.aicodeassistant.retrieval.search;

import java.util.List;

/**
 * Public code-search API (the {@code search} named interface) — the surface other modules (M7 chat)
 * use to retrieve relevant code chunks. The hybrid vector+lexical implementation and its SQL are
 * internal.
 */
public interface CodeSearch {

  /** Returns the most relevant chunks for the query, highest score first. */
  List<SearchResult> search(SearchQuery query);
}
