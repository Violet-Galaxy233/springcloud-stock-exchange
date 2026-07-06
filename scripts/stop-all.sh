#!/usr/bin/env bash
# 停止 scripts/run-all.sh 启动的所有服务。
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
for pidfile in logs/*.pid; do
  [ -e "$pidfile" ] || continue
  pid="$(cat "$pidfile")"
  name="$(basename "$pidfile" .pid)"
  if kill "$pid" 2>/dev/null; then
    echo "stopped $name (pid $pid)"
  fi
  rm -f "$pidfile"
done
