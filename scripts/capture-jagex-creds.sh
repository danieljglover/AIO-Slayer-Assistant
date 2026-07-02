#!/usr/bin/env bash
# Capture the Jagex session that Bolt injects into RuneLite, so the dev client
# (./gradlew run) can log in with your Jagex account.
#
# Usage:
#   1. Open Bolt and click Play on RuneLite. Let it reach the login/loading screen.
#   2. While that client is still running, run this script.
#   3. It writes the JX_* values to scripts/.jagex-env (gitignored).
#   4. Close Bolt's RuneLite, then run scripts/dev-run.sh.
#
# Bolt sets the JX_* variables only on the live game process, so the client
# MUST be running when you run this.
set -euo pipefail

OUT="$(cd "$(dirname "$0")" && pwd)/.jagex-env"
TIMEOUT="${1:-120}"   # seconds to wait for a live client

find_pid() {
  # Find a process (this user) whose environment contains JX_SESSION_ID.
  for pid in $(ls /proc 2>/dev/null | grep -E '^[0-9]+$'); do
    [ -r "/proc/$pid/environ" ] || continue
    if tr '\0' '\n' < "/proc/$pid/environ" 2>/dev/null | grep -q '^JX_SESSION_ID='; then
      echo "$pid"; return 0
    fi
  done
  return 1
}

echo "Waiting up to ${TIMEOUT}s for a running RuneLite launched by Bolt..."
deadline=$(( $(cut -d. -f1 /proc/uptime) + TIMEOUT ))
pid=""
while :; do
  if pid="$(find_pid)"; then break; fi
  [ "$(cut -d. -f1 /proc/uptime)" -ge "$deadline" ] && { echo "Timed out: no client with a JX_SESSION_ID found. Launch RuneLite in Bolt first."; exit 1; }
  sleep 2
done

echo "Found session on PID $pid. Capturing JX_* variables..."
umask 077
: > "$OUT"
tr '\0' '\n' < "/proc/$pid/environ" | grep -E '^JX_' | while IFS= read -r line; do
  echo "export ${line}" >> "$OUT"
done
chmod 600 "$OUT"

echo "Wrote $(grep -c '^export' "$OUT") JX_* variable(s) to: $OUT"
echo "Captured keys (values hidden):"
grep -oE '^export JX_[A-Z_]+' "$OUT" | sed 's/^export /  /'
echo
echo "Now close Bolt's RuneLite and run: scripts/dev-run.sh"
