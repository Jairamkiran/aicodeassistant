# 0014. Citations derived from retrieval; prompt-injection guardrail

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

The flagship chat must ground answers in real code and show **trustworthy**
citations to file:line. Two risks: (1) citations that are fabricated or wrong,
and (2) prompt injection — retrieved repository content is attacker-influenceable
(a repo could contain text like "ignore previous instructions").

## Decision

**Citations are derived from the retrieved chunks, not parsed from the model's
output.** The RAG pipeline numbers the chunks it puts in the prompt (`[1]..[n]`)
and records each chunk's id + file:line as the turn's citations. The model is
asked to reference them by number, but provenance comes from what we retrieved
and showed it — never from scraping `[n]` tokens out of generated text.

**Prompt-injection guardrail:** retrieved content is wrapped in explicit
`<context>...</context>` markers, and the system prompt states the context is
untrusted DATA to be used as reference only — never instructions to follow. This
does not make injection impossible, but it is the standard, low-cost first-line
mitigation and keeps the untrusted/instruction boundary explicit.

## Consequences

- **Positive:** citations are always accurate (they are the sources fed in),
  robust to the model omitting/mangling `[n]` markers; the injection boundary is
  explicit and testable (a unit test asserts the guardrail text + fencing exist).
- **Negative:** a citation may be listed that the model didn't actually use in its
  prose (we cite what was available, not provably what influenced the answer).
  Acceptable — over-citing sources is far safer than fabricating them.
  Guardrailing is mitigation, not a guarantee; defence-in-depth (output
  filtering, allow-listing) can be added later if needed.

## Alternatives considered

- **Parse `[n]` citations from the model's text.** Rejected: fragile (models drop
  or invent markers) and enables fabricated file references — the opposite of
  trustworthy provenance.
- **No guardrail.** Rejected: retrieved repo content is untrusted input entering a
  prompt; leaving the instruction/data boundary implicit is a known injection risk.
