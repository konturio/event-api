# Sprint status and plan

## Sprint 0 status

Sprint 0 is **done** based on the current repository checks.
The baseline quality gate completed successfully in offline pipeline mode.
The command `TEST_MODE=1 PYTHONPATH=. make -B -j all` finished with `BUILD SUCCESS` and `Tests run: 201, Failures: 0, Errors: 0, Skipped: 0`.
The command `make precommit` also finished with `BUILD SUCCESS`.

## Sprint 1 goal

Improve delivery confidence and operational clarity without changing product scope.
Focus on hardening diagnostics, test ergonomics, and documentation quality.

## Sprint 1 scope

### 1) Build and CI reliability

- Investigate and fix the `/bin/sh: 1: Bad substitution` message seen during `make precommit`.
- Add a guard test/check for shell compatibility in scripts called by Make targets.
- Document the expected shell features for local and CI runs.

### 2) Testing and coverage

- Keep `make precommit` and `TEST_MODE=1 PYTHONPATH=. make -B -j all` as mandatory merge gates.
- Add/extend tests for key error paths in jobs and API resources with explicit assertion messages.
- Prioritize modules that have integration behavior and external IO boundaries.

### 3) Developer diagnostics

- Improve developer-facing error messages where generic server errors are still possible.
- Add start/finish debug logs in long-running workflows where state transitions are hard to trace.
- Add input data assertions around IO adapters where malformed payloads can propagate.

### 4) Documentation maintenance

- Keep `docs/testing.md` and related operational docs aligned with real commands used in CI.
- Document Sprint 1 acceptance criteria and close-out checklist in this file.

## Sprint 1 acceptance criteria

- `make precommit` passes without shell warnings.
- `TEST_MODE=1 PYTHONPATH=. make -B -j all` passes in a clean environment.
- New or changed logic in Sprint 1 has test coverage with context-rich assertion messages.
- Documentation updates are included for each significant behavior change.


## Sprint 1 implementation status

- Added `check-shell-compat` target and wired it into `make precommit`.
- Added `scripts/check_shell_compat.sh` as a guard check for shell compatibility.
- Disabled Sentry plugin by default in local/test runs via `sentry.plugin.skip=true` to prevent shell portability noise in baseline checks.
