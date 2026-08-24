# Contributing

Thanks for your interest in contributing to the AI Software Engineering
Assistant. This document explains how to build, test, and submit changes.

## Prerequisites

- **JDK 21** (the Gradle toolchain will auto-provision one if absent)
- **Docker + Docker Compose** — for the infrastructure stack and integration tests
- No Gradle install needed — use the bundled wrapper (`./gradlew`)

## Getting started

```bash
git clone https://github.com/Jairamkiran/aicodeassistant.git
cd aicodeassistant
./gradlew clean check          # compile + unit tests + style + modularity (no Docker)
docker compose up -d           # infra + observability, if you want to run the apps
```

## Development workflow

1. **Branch** off `main`: `git checkout -b feat/<short-name>` or `fix/<short-name>`.
2. **Make focused changes.** Match the surrounding code style; the build enforces
   it (see below).
3. **Keep module boundaries intact.** Do not import another module's `internal`
   packages — `./gradlew :app:test` runs Spring Modulith verification and will
   fail the build on violations.
4. **Add tests.** Unit tests run in `./gradlew test`; Testcontainers integration
   tests go in a module's `src/integrationTest` and run via `./gradlew integrationTest`.
5. **Run the full gate locally** before pushing:
   ```bash
   ./gradlew clean check
   ./gradlew integrationTest    # requires Docker
   ```

## Code style & quality gates

- **Formatting:** [google-java-format](https://github.com/google/google-java-format)
  via Spotless. Run `./gradlew spotlessApply` to auto-format.
- **Static checks:** Checkstyle (config in `config/checkstyle/`).
- **Warnings are errors:** compilation runs with `-Werror`; fix warnings, don't
  suppress them without justification.
- **Architecture:** new bounded contexts follow hexagonal layering
  (`domain` / `application` / `adapter`) — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Commit messages

Use clear, imperative subject lines. [Conventional Commits](https://www.conventionalcommits.org/)
prefixes (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, `chore:`)
are encouraged. Explain the *why* in the body for non-trivial changes.

## Pull requests

- Fill out the PR template.
- Ensure CI is green (build, unit tests, integration tests, image build).
- Keep PRs focused; large sweeping changes are hard to review.
- Reference any related issue.

## Architecture decisions

Significant design decisions are recorded as ADRs in [`docs/adr/`](docs/adr/README.md).
If your change makes or reverses such a decision, add or supersede an ADR.

## Reporting bugs & requesting features

Use the GitHub issue templates. For security issues, **do not** open a public
issue — follow [`SECURITY.md`](SECURITY.md).
