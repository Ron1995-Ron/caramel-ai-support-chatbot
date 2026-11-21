#!/usr/bin/env bash
# Starts the backend and the frontend together, waits until the frontend answers, then opens it in
# Google Chrome. Press Ctrl+C to stop both servers.
#
# Optional environment variables:
#   SERVER_PORT       Backend port (default 8083)
#   FRONTEND_PORT     Frontend port (default 5176)
#   READY_TIMEOUT     Seconds to wait for each server (default 120)
#   NO_BROWSER=1      Do not open a browser
#
# GOOGLE_API_KEY is read from .env by the backend only. This script never reads or prints it.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT="${SERVER_PORT:-8083}"
FRONTEND_PORT="${FRONTEND_PORT:-5176}"
READY_TIMEOUT="${READY_TIMEOUT:-120}"
FRONTEND_URL="http://localhost:${FRONTEND_PORT}"
LOG_DIR="${ROOT}/.run-logs"

PIDS=()

log() { printf '[run.sh] %s\n' "$*"; }
die() { printf '[run.sh] ERROR: %s\n' "$*" >&2; exit 1; }

port_in_use() {
  # True when something is already listening on the given TCP port.
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && { exec 3>&- 3<&-; return 0; }
  return 1
}

cleanup() {
  trap - EXIT INT TERM
  log "Stopping servers..."
  local pid
  for pid in "${PIDS[@]:-}"; do
    [[ -n "$pid" ]] || continue
    # Each server runs in its own process group (setsid), so this stops its children too.
    kill -TERM -- "-${pid}" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# Waits for a URL to answer, or fails early if the server process dies.
wait_for() {
  local name="$1" url="$2" pid="$3" waited=0
  log "Waiting for ${name} at ${url} ..."
  until curl -s -o /dev/null -m 2 "$url"; do
    kill -0 "$pid" 2>/dev/null || die "${name} exited before it was ready. See ${LOG_DIR}/${name}.log"
    (( waited >= READY_TIMEOUT )) && die "${name} was not ready after ${READY_TIMEOUT}s. See ${LOG_DIR}/${name}.log"
    sleep 1
    waited=$((waited + 1))
  done
  log "${name} is ready (${waited}s)."
}

open_chrome() {
  local url="$1" cmd
  for cmd in google-chrome google-chrome-stable; do
    if command -v "$cmd" >/dev/null 2>&1; then
      nohup "$cmd" "$url" >/dev/null 2>&1 &
      return 0
    fi
  done
  if [[ "$(uname -s)" == "Darwin" ]] && open -a "Google Chrome" "$url" 2>/dev/null; then
    return 0
  fi
  if command -v cmd.exe >/dev/null 2>&1 && cmd.exe /c start chrome "$url" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

# --- Checks -------------------------------------------------------------------------------------
command -v java >/dev/null 2>&1 || die "java not found (Java 21+ required)."
command -v mvn  >/dev/null 2>&1 || die "mvn not found."
command -v npm  >/dev/null 2>&1 || die "npm not found."
command -v curl >/dev/null 2>&1 || die "curl not found."
[[ -f "${ROOT}/.env" ]] || die ".env not found in ${ROOT}. Copy .env.example to .env and set GOOGLE_API_KEY."

port_in_use "$BACKEND_PORT" \
  && die "Port ${BACKEND_PORT} is already in use. Free it or run: SERVER_PORT=8081 ./run.sh"
port_in_use "$FRONTEND_PORT" \
  && die "Port ${FRONTEND_PORT} is already in use. Free it or run: FRONTEND_PORT=5174 ./run.sh"

mkdir -p "$LOG_DIR"

if [[ ! -d "${ROOT}/frontend/node_modules" ]]; then
  log "Installing frontend dependencies (first run)..."
  (cd "${ROOT}/frontend" && npm install --no-audit --no-fund) || die "npm install failed."
fi

# --- Start both servers -------------------------------------------------------------------------
log "Starting backend on port ${BACKEND_PORT} (log: .run-logs/backend.log)"
(
  cd "${ROOT}/backend"
  SERVER_PORT="$BACKEND_PORT" exec setsid mvn -q spring-boot:run >"${LOG_DIR}/backend.log" 2>&1
) &
PIDS+=("$!")
BACKEND_PID="$!"

log "Starting frontend on port ${FRONTEND_PORT} (log: .run-logs/frontend.log)"
(
  cd "${ROOT}/frontend"
  VITE_API_TARGET="http://localhost:${BACKEND_PORT}" \
    exec setsid npm run dev -- --port "$FRONTEND_PORT" --strictPort >"${LOG_DIR}/frontend.log" 2>&1
) &
PIDS+=("$!")
FRONTEND_PID="$!"

# Backend first: a missing GOOGLE_API_KEY makes it exit at startup, and that should be reported
# before anything opens in the browser. A 403/404 still counts as "answering".
wait_for backend "http://localhost:${BACKEND_PORT}/api/sessions" "$BACKEND_PID"
wait_for frontend "$FRONTEND_URL" "$FRONTEND_PID"

if [[ "${NO_BROWSER:-0}" == "1" ]]; then
  log "NO_BROWSER=1, not opening a browser."
elif open_chrome "$FRONTEND_URL"; then
  log "Opened ${FRONTEND_URL} in Google Chrome."
else
  log "Could not find Google Chrome. Open ${FRONTEND_URL} yourself."
fi

log "Running. Press Ctrl+C to stop both servers."
# Exit as soon as either server stops.
wait -n "$BACKEND_PID" "$FRONTEND_PID" || true
log "A server stopped. Check the logs in .run-logs/."
