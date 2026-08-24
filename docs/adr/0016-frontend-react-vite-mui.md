# 0016. Frontend stack: React + TypeScript + Vite + MUI + TanStack Query

- **Status:** Accepted
- **Date:** 2026-07
- **Deciders:** Project lead

## Context

M8 adds the first user-facing web client. It must deliver auth flows, a
repository dashboard with live indexing status, hybrid search, and the flagship
**streaming** chat with clickable citations that open code in an editor — all
against the existing REST + SSE backend. The choice needs a mainstream,
well-supported toolchain (the maintainer is backend-first) and must not entangle
the JVM build.

## Decision

A standalone SPA under `frontend/`, separate from the Gradle build:

- **React 18 + TypeScript (strict)** — ubiquitous, typed against the API DTOs.
- **Vite 5** — fast dev server + optimized production build; a dev proxy sends
  `/api` to the app so the SPA and API share an origin (cookies, no CORS).
- **MUI 6** — batteries-included accessible components + light/dark theming.
- **TanStack Query 5** — server-state caching, background refetch, and the
  polling used for live indexing status (no bespoke fetch/loading plumbing).
- **React Router 6** for routing; **Monaco** for code/citation viewing.
- **Vitest + Testing Library** for tests; **ESLint + Prettier** for quality.
- SSE chat uses `fetch` + a manual event-stream parser (not `EventSource`,
  which cannot send an `Authorization` header or a POST body).
- The access token lives only in memory; the refresh token stays an HttpOnly
  cookie. Route-level code splitting keeps the initial bundle small.

## Consequences

- **Positive:** a productive, conventional stack; the frontend builds/tests/lints
  independently in its own CI job and Docker image (nginx); no impact on the JVM
  build graph or Modulith boundaries.
- **Negative:** a second toolchain (Node) to maintain and a second dependency
  surface to keep patched. Accepted — the alternative (a server-rendered Java UI)
  would couple the UI to the backend deployable and fit the SPA/SSE UX poorly.

## Alternatives considered

- **Server-side rendering (Thymeleaf) in the app.** Rejected: couples UI to the
  monolith, awkward for token-streaming chat, and mixes concerns in one deployable.
- **Next.js.** Rejected as overkill — there is no SSR/SEO requirement; a static
  SPA behind nginx is simpler to build, ship, and reason about.
