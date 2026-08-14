#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_ROOT="$SCRIPT_DIR/maxkey-web-frontend"
UNIFIED_APP="$WEB_ROOT/maxkey-web-vue-app"

install_dependencies() {
  local app_dir="$1"
  local app_name="$2"

  if [[ -d "$app_dir/node_modules" ]]; then
    return
  fi

  echo "[$app_name] Installing dependencies..."
  npm install \
    --prefix "$app_dir" \
    --ignore-scripts \
    --no-audit \
    --no-fund \
    --package-lock=false \
    --registry "${NPM_REGISTRY:-https://registry.npmmirror.com}"
}

stop_servers() {
  trap - EXIT INT TERM
  echo
  echo "Stopping frontend servers..."
  kill "${UNIFIED_PID:-}" 2>/dev/null || true
  wait "${UNIFIED_PID:-}" 2>/dev/null || true
}

install_dependencies "$UNIFIED_APP" "maxkey-web-vue-app"

trap stop_servers EXIT INT TERM

echo "Unified Vue portal: http://127.0.0.1:8527/maxkey/"
(
  cd "$UNIFIED_APP"
  exec npm run dev -- --host 127.0.0.1 --port 8527
) &
UNIFIED_PID=$!

echo "Press Ctrl+C to stop both servers."
wait "$UNIFIED_PID"
