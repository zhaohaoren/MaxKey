#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_ROOT="$SCRIPT_DIR/maxkey-web-frontend"
USER_APP="$WEB_ROOT/maxkey-web-app"
MGT_APP="$WEB_ROOT/maxkey-web-mgt-app"

install_dependencies() {
  local app_dir="$1"
  local app_name="$2"

  if [[ -x "$app_dir/node_modules/.bin/ng" ]]; then
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
  kill "${USER_PID:-}" "${MGT_PID:-}" 2>/dev/null || true
  wait "${USER_PID:-}" "${MGT_PID:-}" 2>/dev/null || true
}

install_dependencies "$USER_APP" "maxkey-web-app"
install_dependencies "$MGT_APP" "maxkey-web-mgt-app"

trap stop_servers EXIT INT TERM

echo "User portal: http://127.0.0.1:8527/maxkey/"
(
  cd "$USER_APP"
  exec ./node_modules/.bin/ng serve \
    --serve-path=/maxkey/ \
    --port=8527 \
    --host=127.0.0.1
) &
USER_PID=$!

echo "Management portal: http://127.0.0.1:8526/maxkey-mgt/"
(
  cd "$MGT_APP"
  exec ./node_modules/.bin/ng serve \
    --serve-path=/maxkey-mgt/ \
    --port=8526 \
    --host=127.0.0.1
) &
MGT_PID=$!

echo "Press Ctrl+C to stop both servers."
wait "$USER_PID" "$MGT_PID"
