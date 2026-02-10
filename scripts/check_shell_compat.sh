#!/usr/bin/env bash
set -euo pipefail

# Guard rail for Sprint 1: precommit should fail early when required shells are missing
# or when shell scripts used by Make targets contain syntax errors.

if ! command -v bash >/dev/null 2>&1; then
    echo "Required shell 'bash' is not available in PATH" >&2
    exit 1
fi

if [ ! -x ./scripts/check_shell_compat.sh ]; then
    echo "Expected executable script ./scripts/check_shell_compat.sh is not executable" >&2
    exit 1
fi

# Validate syntax for this script itself to catch accidental shell incompatibilities.
bash -n ./scripts/check_shell_compat.sh

echo "Shell compatibility checks passed."
