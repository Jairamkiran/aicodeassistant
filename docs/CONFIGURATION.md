# Configuration Guide

All configuration is environment-variable driven with development-only fallbacks
in `application.yml`. Nothing sensitive is hard-coded; production supplies
secrets via Kubernetes secrets. Activate production hardening with
`SPRING_PROFILES_ACTIVE=prod` (see `application-prod.yml`).

## Secrets — **must** be overridden in production

| Variable | Default (dev only) | Purpose |
| --- | --- | --- |
| `AICODEASSISTANT_DB_PASSWORD` | `aicodeassistant` | Database password. |
| `AICODEASSISTANT_JWT_SECRET` | dev placeholder (≥32 B) | HS256 signing key for access tokens. |
| `AICODEASSISTANT_CRYPTO_KEY` | dev placeholder | Base64 256-bit AES key for encrypting provider tokens at rest. |
| `AICODEASSISTANT_GITHUB_CLIENT_ID` / `_SECRET` | empty | GitHub OAuth app credentials. |
| `AICODEASSISTANT_OPENAI_API_KEY` | empty | Required only when the chat provider is `openai`. |

## Datasource & infrastructure

| Variable | Default | Purpose |
| --- | --- | --- |
| `AICODEASSISTANT_DB_URL` | `jdbc:postgresql://localhost:5432/aicodeassistant` | JDBC URL. |
| `AICODEASSISTANT_DB_USER` | `aicodeassistant` | DB user. |
| `AICODEASSISTANT_DB_POOL_MAX` | `10` (`20` in prod) | Hikari max pool size. |
| `AICODEASSISTANT_KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka bootstrap servers. |
| `AICODEASSISTANT_REDIS_HOST` / `_PORT` | `localhost` / `6379` | Redis (rate limiting). |

## AI providers

| Variable | Default | Purpose |
| --- | --- | --- |
| `AICODEASSISTANT_CHAT_PROVIDER` | `ollama` | Chat backend: `ollama` (local, default) or `openai`. |
| `AICODEASSISTANT_OLLAMA_URL` | `http://localhost:11434` | Ollama base URL. |
| `AICODEASSISTANT_OLLAMA_CHAT_MODEL` | `llama3.1` | Ollama chat model. |
| `AICODEASSISTANT_OLLAMA_EMBED_MODEL` | `nomic-embed-text` | Embedding model. |
| `AICODEASSISTANT_OLLAMA_EMBED_DIM` | `768` | Embedding dimension (must match the `code_chunks` vector column). |
| `AICODEASSISTANT_OPENAI_URL` | `https://api.openai.com/v1` | OpenAI base URL. |
| `AICODEASSISTANT_OPENAI_CHAT_MODEL` | `gpt-4o-mini` | OpenAI chat model. |

## Retrieval & RAG tuning

| Variable | Default | Purpose |
| --- | --- | --- |
| `AICODEASSISTANT_RETRIEVAL_RRF_K` | `60` | Reciprocal-rank-fusion damping constant. |
| `AICODEASSISTANT_RETRIEVAL_VECTOR_WEIGHT` | `1.0` | Fusion weight for the semantic retriever. |
| `AICODEASSISTANT_RETRIEVAL_LEXICAL_WEIGHT` | `1.0` | Fusion weight for the lexical retriever. |
| `AICODEASSISTANT_CHAT_RETRIEVAL_LIMIT` | `8` | Chunks retrieved as chat context. |
| `AICODEASSISTANT_CHAT_MEMORY_TURNS` | `6` | Windowed conversation-memory turns. |
| `AICODEASSISTANT_CHAT_CONTEXT_BUDGET` | `12000` | Max characters of context packed into the chat prompt. |
| `AICODEASSISTANT_REVIEW_RETRIEVAL_LIMIT` | `12` | Chunks retrieved for a code review. |
| `AICODEASSISTANT_REVIEW_CONTEXT_BUDGET` | `14000` | Max characters of context in the review prompt. |

## Identity & tokens

| Variable | Default | Purpose |
| --- | --- | --- |
| `AICODEASSISTANT_ACCESS_TOKEN_TTL` | `PT15M` | Access-token lifetime. |
| `AICODEASSISTANT_REFRESH_TOKEN_TTL` | `P14D` | Refresh-token lifetime. |
| `AICODEASSISTANT_JWT_ISSUER` | `aicodeassistant` | JWT issuer claim. |
| `AICODEASSISTANT_REFRESH_COOKIE_NAME` | `aicodeassistant_refresh` | Refresh-cookie name. |
| `AICODEASSISTANT_REFRESH_COOKIE_SECURE` | `false` (`true` in prod) | Secure flag on the refresh cookie. |

Background maintenance intervals (refresh-token cleanup, stalled-index-job
reaper) are configurable via `aicodeassistant.iam.token-cleanup.*` and
`aicodeassistant.indexing.reaper.*`; their defaults are sensible and rarely need
changing.

## Observability

| Variable | Default | Purpose |
| --- | --- | --- |
| `AICODEASSISTANT_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP trace endpoint (Tempo). |
| `AICODEASSISTANT_TRACE_SAMPLING` | `1.0` (`0.1` in prod) | Trace sampling probability. |

## Frontend

The SPA build reads `VITE_API_PROXY_TARGET` (dev proxy target, default
`http://localhost:8080`). In containers, nginx proxies `/api` to the `app`
service; no build-time API URL is baked in.
