# 0010. Line-based text chunking for M4 (defer AST-aware chunking)

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

The indexing saga must split source files into chunks to embed. Two approaches:
language-aware AST/tree-sitter chunking (split on function/class boundaries), or
line-based windowing (fixed line windows with overlap).

## Decision

Ship **line-based windowing** in M4: windows of N lines (default 60) with an
overlap (default 10) so a construct spanning a boundary still appears whole in
some chunk. Language is tagged by file extension for metadata/filtering, but the
split itself is not language-aware. Binary/oversized files are excluded by the
cloner.

AST-aware chunking is **deferred** until M5's retrieval-quality evaluation shows
it is needed — building N-language parsers now is speculative generality.

## Consequences

- **Positive:** simple, language-agnostic, works for every file type immediately;
  overlap preserves cross-boundary context; it is what most production RAG
  pipelines ship first. Fully unit-testable.
- **Negative:** chunks can split mid-function, which can dilute embedding
  relevance for some queries. Acceptable for M4; if M5 retrieval metrics show it
  hurts, AST chunking becomes a justified, measured enhancement (the `Chunker` is
  an isolated component, easy to swap).

## Alternatives considered

- **AST/tree-sitter now.** Rejected as premature: multi-language parser
  integration and upkeep with no evidence yet that line-chunking is insufficient.
- **Whole-file embedding.** Rejected: large files exceed model context and bury
  relevant snippets; retrieval granularity suffers.
