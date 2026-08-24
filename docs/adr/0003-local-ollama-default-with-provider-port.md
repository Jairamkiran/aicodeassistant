# 0003. Local Ollama as the default AI provider behind a port

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

The product is LLM-centric. For a public showcase, anyone who clones it should be
able to run the full experience without signing up for a paid API or leaking
code to a third party. At the same time, production users will want managed,
higher-quality models (OpenAI, Azure OpenAI, Bedrock).

## Decision

Default the local/dev/demo stack to a **local Ollama** instance (`llama3.1`
chat, `nomic-embed-text` embeddings), pulled automatically by docker-compose.
Access all models through provider-agnostic ports (`ChatModelPort`,
`EmbeddingModelPort`, introduced in M6) implemented over Spring AI / LangChain4j,
so switching to OpenAI/Azure/Bedrock is a configuration change, not a code
change.

## Consequences

- **Positive:** zero-friction, free, offline, privacy-preserving demo; clean
  Dependency-Inversion seam; provider choice is a deployment concern.
- **Negative:** local model quality is lower than hosted frontier models, and
  Ollama needs a few GB of disk/RAM. Documented in the README; users can flip to
  a hosted provider via config.

## Alternatives considered

- **OpenAI-compatible cloud as the default.** Rejected as the *default*: requires
  an API key and spend to run the demo, blocking casual reviewers. Fully
  supported as a configured provider.
- **Hard-coding a single provider.** Rejected: violates the Dependency Inversion
  Principle and locks the product to one vendor.
