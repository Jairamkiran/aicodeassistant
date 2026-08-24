# 0008. AES-256-GCM for provider tokens at rest

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

Linking GitHub (ADR-0007) requires storing an OAuth access token that the system
must later present back to GitHub. Unlike passwords (hashed, one-way) or refresh
tokens (hashed, high-entropy), a provider token must be **recoverable** — so it
must be encrypted, not hashed. M0–M2 introduced no encryption primitive.

## Decision

Add an `EncryptionService` in the `platform` shared kernel, implemented with
**AES-256-GCM** (`AesGcmEncryptionService`):

- Fresh random 96-bit IV per encryption; 128-bit authentication tag.
- Ciphertext is a self-describing string `v1:<b64url(iv)>:<b64url(ct||tag)>`; the
  `v1` prefix allows future key rotation / algorithm change.
- Key is a Base64-encoded 256-bit value from configuration
  (`aicodeassistant.crypto.key`), supplied via secret in real deployments with a
  clearly non-production dev fallback.
- Tampering or a wrong key fails on decrypt with `DecryptionException` (GCM tag
  check) rather than returning corrupt data.

## Consequences

- **Positive:** provider tokens are never stored in plaintext; authenticated
  encryption detects tampering; the versioned format enables rotation; the
  primitive is reusable for any future recoverable secret.
- **Negative:** introduces key management — the key must be provisioned and
  protected, and losing it makes stored tokens unrecoverable (users must
  re-link). This is inherent to symmetric encryption and accepted. Key rotation
  tooling (re-encrypt with a new `v2` key) is deferred until needed.

## Alternatives considered

- **Hashing.** Impossible — the token must be sent back to GitHub, so it cannot
  be one-way hashed.
- **Plaintext + "encrypt later".** Rejected — ships an insecure default,
  contradicting the project's no-insecure-defaults bar.
- **A KMS / envelope encryption.** Deferred — appropriate at cloud-deploy scale
  (M15), heavier than justified now; the `v1:` prefix leaves room to adopt it.
