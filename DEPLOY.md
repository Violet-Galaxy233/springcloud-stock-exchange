# 本地部署指南

在本机从零把 Warp Exchange 跑起来。整体三步：**启动中间件 → 打包 → 按顺序启动服务**。

## 一、前置条件

| 依赖 | 版本 | 验证命令 |
|------|------|----------|
| JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker Desktop | 运行中 | `docker info` |

> 端口占用：请确保本机 `2181 / 3306 / 6379 / 9092`（中间件）与
> `8000~8005 / 8888`（各服务）未被占用。

## 二、启动基础中间件

在项目根目录执行：

```bash
docker compose up -d      # 启动 Kafka(+ZooKeeper)、Redis、MySQL
docker compose ps         # 查看状态，等各容器 Up（约 20~30 秒）
```

- MySQL **首次**启动会自动执行 `sql/schema.sql` 建库建表（数据库名 `exchange`，root 口令 `password`）。
- 如需重置数据：`docker compose down -v` 会连同数据卷一起删除，下次 `up` 重新建表。

## 三、编译打包

```bash
cd build && mvn clean package    # 编译全部模块 + 运行 16 个测试
cd ..
```

产物为各服务的可执行 jar：`config/target/config.jar`、`trading-engine/target/trading-engine.jar` 等。

## 四、启动服务

各服务需从**配置中心**读取配置，因此 **`config`（8888）必须最先启动**。

### 方式 A：一键脚本（推荐）

```bash
scripts/run-all.sh        # 先起 config 并等待其就绪，再起其余 6 个服务
```

- 日志写入 `logs/<服务名>.log`，进程 PID 写入 `logs/<服务名>.pid`。
- 停止：`scripts/stop-all.sh`。

### 方式 B：手动 / IDE

先启动配置中心，再启动其余（顺序建议 engine/sequencer/quotation/push → api → ui）：

```bash
java -jar config/target/config.jar              # 8888，等它 UP 再继续
java -jar trading-engine/target/trading-engine.jar
java -jar trading-sequencer/target/trading-sequencer.jar
java -jar quotation/target/quotation.jar
java -jar push/target/push.jar
java -jar trading-api/target/trading-api.jar
java -jar ui/target/ui.jar
```

在 IntelliJ IDEA 中：先运行 `ConfigApplication`，其余各 `*Application` 随后运行即可。

## 五、验证

| 服务 | 地址 |
|------|------|
| Web UI | http://localhost:8000 |
| 交易 API 健康检查 | http://localhost:8001/actuator/health |
| 配置中心（示例） | http://localhost:8888/trading-engine/default |
| 行情 K 线（示例） | http://localhost:8004/api/quotation/bars/min |

在 UI 上：**注册 → 登录 → Deposit(demo) 充值 → 下单买卖**，即可看到资产、订单簿、
活动订单实时变化；发生成交后 K 线开始出现数据。

## 六、端口一览

| 组件 | 端口 |
|------|------|
| config（配置中心） | 8888 |
| ui | 8000 |
| trading-api | 8001 |
| trading-engine | 8002 |
| trading-sequencer | 8003 |
| quotation | 8004 |
| push | 8005 |
| MySQL / Redis / Kafka / ZooKeeper | 3306 / 6379 / 9092 / 2181 |

## 七、常见问题

- **`Unable to load config data from 'configserver:http://localhost:8888'`**
  配置中心没起或没最先起。先确认 `curl http://localhost:8888/actuator/health` 返回 UP。

- **数据库连接被拒 / 表不存在**
  MySQL 还没就绪，或数据卷是旧的没执行建表脚本。等容器 Up；必要时
  `docker compose down -v && docker compose up -d` 重建。

- **启动瞬间大量 Kafka 连接告警**
  中间件还没完全就绪，Spring Kafka 会自动重试，稍等即可；也可先等
  `docker compose ps` 全部 Up 再启动服务。

- **端口被占用**
  改对应服务的端口：在 `config-repo/<服务>.yml` 里改 `server.port`，或用环境变量
  （如 `APP_PORT` 之于 ui）覆盖，无需改代码。

- **想连到非本机的中间件**
  各服务支持环境变量覆盖：`MYSQL_HOST`/`MYSQL_PORT`/`MYSQL_USER`/`MYSQL_PASSWORD`、
  `REDIS_HOST`/`REDIS_PORT`、`KAFKA_SERVERS`、`CONFIG_SERVER`。
  例如 `KAFKA_SERVERS=10.0.0.5:9092 java -jar trading-engine/target/trading-engine.jar`。

## 八、快速自检（无需中间件）

只想确认代码没问题，可只跑交易引擎测试（含内嵌 Kafka，不依赖 Docker）：

```bash
mvn -f trading-engine/pom.xml test
```
