#!/usr/bin/env bash
# Launch the development RuneLite client with the All-In Slayer plugin loaded,
# using the Jagex session captured by scripts/capture-jagex-creds.sh.
#
# Usage:
#   scripts/dev-run.sh
#
# If login fails because the session expired, re-run scripts/capture-jagex-creds.sh
# (with RuneLite open in Bolt) to refresh .jagex-env, then run this again.
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$DIR")"
ENV_FILE="$DIR/.jagex-env"

if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  echo "Loaded Jagex session for: ${JX_DISPLAY_NAME:-<unknown>}"
else
  echo "No $ENV_FILE found - the dev client will start without a Jagex login."
  echo "Run scripts/capture-jagex-creds.sh first (with RuneLite open in Bolt)."
fi

cd "$ROOT"
exec ./gradlew run
