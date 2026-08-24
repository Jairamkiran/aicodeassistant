<!-- Thanks for contributing! Keep PRs focused and CI green. -->

## Summary

<!-- What does this change and why? Link the issue it closes. -->

Closes #

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor / cleanup
- [ ] Documentation
- [ ] Build / CI / infrastructure

## Checklist

- [ ] `./gradlew clean check` passes locally (compile, unit tests, Spotless, Checkstyle, Modulith verify)
- [ ] `./gradlew integrationTest` passes locally, or N/A (no infra-touching change)
- [ ] No new cross-module `internal` package imports (module boundaries intact)
- [ ] Tests added/updated for the change
- [ ] Public APIs have Javadoc; new config keys documented
- [ ] Docs updated (README / ARCHITECTURE / ADR) if behavior or design changed
- [ ] No secrets committed; new config externalized via environment variables

## Notes for reviewers

<!-- Anything specific you want scrutiny on; trade-offs; follow-ups. -->
