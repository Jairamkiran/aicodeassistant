# API Reference

The backend exposes a versioned REST API under `/api/v1`. All endpoints return
JSON; errors follow **RFC 9457 problem+json**. The live, always-accurate
contract is the OpenAPI document at `/v3/api-docs` (Swagger UI:
`/swagger-ui.html`). This page is a hand-maintained overview.

## Authentication

- **JWT bearer** (`Authorization: Bearer <accessToken>`) for user sessions.
  Access tokens are short-lived; a rotating refresh token is delivered as a
  Secure/HttpOnly cookie and exchanged at `/auth/refresh`.
- **API keys** (`X-API-Key: aca_<prefix>.<secret>`) for programmatic access.

Rate limiting is applied per principal (Redis token bucket; fails open).

## Endpoints

### Auth — `/api/v1/auth`
| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/register` | none | Create an account; returns access token + sets refresh cookie. |
| POST | `/login` | none | Authenticate; returns access token + sets refresh cookie. |
| POST | `/refresh` | refresh cookie | Rotate the refresh token; returns a new access token. |
| POST | `/logout` | bearer | Revoke the refresh-token family; clears the cookie. |

### Users & organizations
| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/users/me` | Current user profile. |
| GET | `/api/v1/organizations` | Organizations the caller belongs to. |
| POST | `/api/v1/organizations/{organizationId}/members` | Add/re-role a member (ADMIN+). |

### Repositories — `/api/v1/repositories`
| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/github` | List the caller's importable GitHub repositories. |
| POST | `/import` | Import a GitHub repo (`owner`, `name`) into an org (MEMBER+). |
| GET | `?organizationId=` | List imported repositories in an org. |
| GET | `/{id}` | Fetch one repository (status drives the UI). |
| POST | `/{id}/reindex` | Re-run indexing (MEMBER+). |
| DELETE | `/{id}` | Delete a repository and its indexed data (MEMBER+). |

### Search — `/api/v1/search`
| Method | Path | Purpose |
| --- | --- | --- |
| POST | `` | Hybrid vector+lexical search. Body: `organizationId`, `query`, optional `repositoryId`, `language`, `limit`. Returns hits with file:line provenance. |

### Chat (RAG) — `/api/v1/chat`
| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/sessions` | Create a chat session over a repository. |
| GET | `/sessions?organizationId=` | List the caller's sessions. |
| GET | `/sessions/{id}` | Fetch a session with its turns. |
| PATCH | `/sessions/{id}` | Rename a session. |
| DELETE | `/sessions/{id}` | Delete a session. |
| POST | `/sessions/{id}/messages` | Ask a question. **SSE stream**: `token` events, then `citations`, then `done`. |

### Code review — `/api/v1/code-reviews`
| Method | Path | Purpose |
| --- | --- | --- |
| POST | `` | Structured AI review of a repository for a focus topic. Returns a summary + severity-ordered findings (file/line). |

### Notifications — `/api/v1/notifications`
| Method | Path | Purpose |
| --- | --- | --- |
| GET | `?limit=` | Recent notifications, newest first. |
| GET | `/unread-count` | Unread count. |
| POST | `/{id}/read` | Mark a notification read. |

### API keys — `/api/v1/api-keys`
| Method | Path | Purpose |
| --- | --- | --- |
| GET | `` | List the caller's API keys. |
| POST | `` | Create a key (secret returned once). |
| DELETE | `/{id}` | Revoke a key. |

### GitHub integration — `/api/v1/integrations/github`
| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/authorize` | Begin the GitHub OAuth flow. |
| GET | `/callback` | OAuth callback; stores the encrypted token. |

## Error model (RFC 9457)

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "You cannot access this organization",
  "instance": "/api/v1/search",
  "correlationId": "e4c1…"
}
```

Validation failures include a field→message `errors` map. Every response carries
an `X-Correlation-Id` header for tracing.
