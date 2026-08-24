# 0015. Provider-agnostic structured output (prompt + parse, no vendor JSON mode)

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M9 needs the model to return machine-readable results (the first consumer is
Code Review: a list of findings with severity/file/line). ADR-0012 deferred
"structured-output parsing" to this milestone precisely because there was no
consumer before. Two provider-specific mechanisms exist — OpenAI's
`response_format: json_schema` and Ollama's `format: json` — but they differ in
capability and neither is available on every model (small local models follow a
formal JSON Schema poorly). Adding a vendor JSON-mode field to `ChatRequest`
would also leak a provider concept into the shared `chat` port.

## Decision

Do structured output with **prompt engineering + robust parsing**, entirely in
the `ai :: chat` named interface, working identically on both providers:

- `StructuredOutputs.jsonInstruction(schemaHint)` — a reusable system-prompt
  fragment telling the model to emit a single JSON document matching a
  human-readable shape hint (field names + types), no prose, no fences.
- `StructuredOutputs.parse(reply, type, mapper)` — extracts the **first balanced
  JSON value** from the reply (respecting string literals and escapes, so braces
  inside strings don't confuse the matcher; tolerant of ```json fences and
  surrounding prose) and deserializes it, throwing `StructuredOutputException`
  when the payload is absent or non-conformant.

The caller (`codeintel` Code Review) maps the parsed JSON via a **private DTO**
to its domain types, so no JSON shape crosses a module boundary. `ChatRequest`
is unchanged — no vendor JSON-mode field.

## Consequences

- **Positive:** one mechanism for all providers/models incl. local Ollama; no
  provider concept leaks into the port; fully unit-testable without a live model
  (pure string→object). Robust to the real-world "model wraps JSON in prose"
  behaviour.
- **Negative:** slightly less airtight than a native constrained decoder — a model
  can still emit invalid JSON, which surfaces as a clear `StructuredOutputException`
  the caller handles (HTTP 502 via the error model). Acceptable: the feature is a
  read/advisory path, not a transaction.

## Alternatives considered

- **Native JSON mode per provider.** Rejected for now: uneven model support and
  it pushes a provider concept into the shared request type. Can be added later as
  an internal adapter optimisation behind the same `StructuredOutputs` contract if
  a provider/model guarantees it.
- **A full JSON-Schema validator + repair loop.** Rejected as over-engineering for
  the current consumers; the tolerant extractor + typed parse covers observed
  model behaviour. Revisit if structured features multiply.
