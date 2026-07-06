#!/usr/bin/env bash
#
# 依次启动 Warp Exchange 的所有微服务 (需先 `docker compose up -d` 启动中间件，
# 并已执行 `cd build && mvn clean package` 打包)。
#
# 每个服务在后台运行，日志写入 logs/ 目录。停止：scripts/stop-all.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
mkdir -p logs

start() {
  local name="$1"; local jar="$2"; shift 2
  echo "starting $name ..."
  nohup java -jar "$jar" "$@" > "logs/$name.log" 2>&1 &
  echo $! > "logs/$name.pid"
}

# 1) 配置服务器优先启动，其它服务启动时需从它读取配置:
start config        config/target/config.jar
echo "waiting for config server (8888) ..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:8888/actuator/health >/dev/null 2>&1; then break; fi
  sleep 1
done

# 2) 其余服务 (顺序不敏感，但引擎/定序应先于 API):
start trading-engine    trading-engine/target/trading-engine.jar
start trading-sequencer trading-sequencer/target/trading-sequencer.jar
start quotation         quotation/target/quotation.jar
start push              push/target/push.jar
start trading-api       trading-api/target/trading-api.jar
start ui                ui/target/ui.jar

echo
echo "all services launched. open http://localhost:8000"
echo "logs are in ./logs/, stop with scripts/stop-all.sh"
