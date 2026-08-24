# Security Policy

## Supported versions

This project is under active, pre-release development. Security fixes are applied
to the `main` branch. There is no released version line yet; once releases begin,
this table will list supported versions.

| Version | Supported |
| --- | --- |
| `main` (unreleased) | ✅ |

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
discussions, or pull requests.**

Instead, use **[GitHub's private vulnerability reporting](https://github.com/Jairamkiran/aicodeassistant/security/advisories/new)**:
1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability**.
3. Provide a description, reproduction steps, affected components, and impact.

If private reporting is unavailable, contact the maintainer
(**Jairamkiran Vasupalli**, [@Jairamkiran](https://github.com/Jairamkiran)) directly.

### What to expect

- **Acknowledgement** within 5 business days.
- An assessment and, if accepted, a remediation plan with a target timeline.
- Credit in the fix's release notes if you wish (coordinated disclosure).

## Scope

In scope: the application code, build, container images, and default
configuration in this repository.

Out of scope: vulnerabilities in third-party dependencies (report those upstream;
we track them via Dependabot), and issues that require a compromised host or
non-default insecure configuration.

## Security practices in this project

- Secrets are never committed; configuration is externalized via environment
  variables (development-only fallbacks are clearly marked and must be overridden
  in any real deployment).
- Containers run as a non-root user.
- Error responses are sanitized — internal exception details are never returned
  to clients.
- Dependencies are monitored by Dependabot; CI runs on every PR.
