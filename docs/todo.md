# Iteration notes and impediments

## 2026-02-10

- Resolved:
  `make precommit` no longer emits `/bin/sh: 1: Bad substitution` after making Sentry plugin opt-in for local/test checks.

- Follow-up:
  Verify CI/release jobs that require Sentry uploads set `-Dsentry.plugin.skip=false` explicitly.
