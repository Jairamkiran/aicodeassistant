# 0007. GitHub as an external dependency (OAuth + REST)

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M3 introduces the first external provider: GitHub. Users link their account so
the product can list and import their repositories. External calls are the top
source of production incidents, so this integration must be built to the
project's standing integration directive (handle rate limits, retries, timeouts,
partial failures; no provider types leak; observable; ADR per dependency).

## Decision

- **Linking:** OAuth 2.0 web flow (authorize redirect → callback → code/token
  exchange). Chosen over PATs as the primary, SaaS-credible mechanism.
- **Token at rest:** encrypted with AES-256-GCM (see ADR-0008); never stored or
  logged in plaintext.
- **Encapsulation:** all GitHub specifics — HTTP client, JSON DTOs, OAuth, the
  encrypted token — live in `integration/github/internal`. Other modules use
  only the `github` named-interface (`GitHubGateway` + the neutral `GitHubRepo`
  record). The token is resolved by userId inside the module, so no credential
  or provider type crosses a boundary.
- **Resilience:** Resilience4j `@Retry` + `@CircuitBreaker` on the gateway, and
  connect/read timeouts on the HTTP client. Retries and the breaker count ONLY
  transient failures (`GitHubException.Unavailable`); credential/not-linked
  errors are never retried.
- **Transport:** HTTP/1.1 pinned on the JDK client (avoids an HTTP/2 RST_STREAM
  edge case; GitHub's API is HTTP/1.1-friendly).
- **Observability:** every call is timed + tagged (`github.api.call`,
  operation/outcome) via Micrometer, and transport failures are logged at WARN.

### Failure modes & recovery

| Condition | Mapped to | Retried? | Breaker? | Client sees |
| --- | --- | --- | --- | --- |
| 200 | domain result | — | — | data |
| 401 (bad/expired token) | `CredentialRejected` | no | no | 409 "re-link" |
| 403 (forbidden/rate) | `Unavailable` | yes | yes | 503 |
| 404 | `Unavailable` (with detail) | no | no | 503/handled by caller |
| 429 (rate limit) | `Unavailable` | yes | yes | 503 + retry later |
| 5xx | `Unavailable` | yes | yes | 503 |
| timeout / connection error | `Unavailable` | yes | yes | 503 |
| OAuth code invalid (200 + error body) | `CredentialRejected` | no | no | 409 |

Recovery: transient failures are retried with exponential backoff; if failures
persist the circuit breaker opens for 30s (half-open probe restores traffic),
shielding both us and GitHub. Credential rejection surfaces a "re-link GitHub"
signal to the user.

## Consequences

- **Positive:** domain stays provider-agnostic; failures degrade gracefully;
  calls are observable; the seam to add a second provider later is clear.
- **Negative:** live linking needs a registered GitHub OAuth App (client
  id/secret via secret). Verified in tests against WireMock, so CI needs no real
  GitHub. Only the first page of repos is listed in M3 (pagination is a noted
  enhancement, not built speculatively).

## Alternatives considered

- **Personal Access Tokens as primary.** Rejected as primary (less SaaS-like);
  may be added later as an alternative for self-hosted use.
- **A `VcsProvider` abstraction now.** Rejected — only GitHub exists; per the
  integration directive we do not add the port until a second provider does.
