# Warp Exchange · Spring Cloud 证券交易所

一个基于 **Spring Cloud** 的 7x24 小时运行的证券/加密货币交易所示例项目（BTC/USD 交易对），
参考 [廖雪峰《Java 教程 · Spring Cloud 开发》](https://liaoxuefeng.com/books/java/) 的架构设计实现。

项目采用**事件驱动**架构，核心是一个 **100% 全内存、确定性状态机**的撮合交易引擎，
配合定序、行情、推送、API、Web UI 等微服务，完整演示了一个分布式交易系统的骨架。

> 用途：面试作品 / 分布式系统与撮合引擎学习。

---

## 目录

- [系统架构](#系统架构)
- [模块说明](#模块说明)
- [核心设计](#核心设计)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [REST API](#rest-api)
- [测试](#测试)
- [面试要点](#面试要点)

---

## 系统架构

```
                                query (订单簿/资产/订单)
              ┌───────────────────────────────────────────────┐
              │                                                ▼
  ┌────────┐  │  ┌────────┐   sequence  ┌───────────┐  trade  ┌────────┐
  │ Client │──┼─▶│  API   │────────────▶│ Sequencer │────────▶│ Engine │
  └────────┘  │  └────────┘   (Kafka)   └───────────┘ (Kafka) └────────┘
       ▲       │      ▲                                            │
       │       │      │                                       tick │ (Kafka)
  ┌─────────┐  │  ┌────────┐                                       ▼
  │ Browser │──┼─▶│   UI   │                                 ┌───────────┐
  └─────────┘  │  └────────┘                                 │ Quotation │
       ▲       │                                             └───────────┘
       │       │  notification (Redis Pub/Sub)  ┌──────┐          │
       └───────┴────────── WebSocket ───────────│ Push │◀─────────┘
                                                └──────┘   (orderbook/notify via Redis)

  基础设施：MySQL 8（持久化）· Redis 7（订单簿快照/行情缓存/推送）· Kafka（消息流）
  配置中心：Spring Cloud Config（native 模式，读取 config-repo/）
```

**下单/撤单/充值**（写）走 Kafka 异步链路：`API → Sequencer → Engine`，由定序服务赋予全局有序 ID；
**订单簿/资产/订单查询**（读）由 API 直接同步调用交易引擎或读取 Redis 快照。

---

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| `common` | — | 公共代码：领域模型、枚举、消息、Kafka/Redis 封装、工具类 |
| `config` | 8888 | Spring Cloud Config 配置服务器（native，读取 `config-repo/`） |
| `trading-api` | 8001 | 交易 API：下单/撤单/充值入口 + 订单簿/资产/历史查询 + 用户认证 |
| `trading-engine` | 8002 | **交易引擎**：资产、订单、撮合、清算（全内存确定性状态机） |
| `trading-sequencer` | 8003 | 定序服务：为事件分配全局有序 ID 并持久化，转发给引擎 |
| `quotation` | 8004 | 行情服务：消费 Tick 聚合为秒/分/时/日 K 线 |
| `push` | 8005 | 推送服务：订阅 Redis，经 WebSocket 将通知推给客户端 |
| `ui` | 8000 | 用户 Web 界面（Pebble 模板 + 原生 JS，零外部依赖） |
| `parent` / `build` | — | Maven 父 POM / 聚合构建模块 |
| `config-repo` | — | 各服务的配置文件（被 Config Server 读取） |

---

## 核心设计

### 1. 交易引擎 = 确定性状态机

交易引擎的输入是一条**定序后的确定事件序列** `[O₁, O₂, O₃, ...]`，
内部状态（资产表、订单集、订单簿）随之确定性更新，输出为成交明细与行情。

> 当前状态 Sₙ，给定确定输入 Oₙ₊₁，下一个状态 Sₙ₊₁ 完全确定，与时间无关。

因此，对同一组输入，任意多个引擎实例都会得到**完全相同**的输出 —— 这正是实现
高可用（多实例热备）与故障恢复（重放事件）的基础。引擎全内存运行，TPS 极高，
持久化在主流程之外异步批量进行。

### 2. 资产系统：一切皆转账

用户资产是「用户 ID + 资产 ID」标识的二维表，每格含**可用**与**冻结**两个余额。
所有资产操作本质只有一个方法 —— `tryTransfer()`，三种类型：可用↔可用、可用→冻结、冻结→可用。

引入一个 **系统负债账户（ID=1）** 记录所有用户权益，保证整个系统的
**资产负债表恒为零**，从而随时可对账。测试中对任意操作序列都校验了这一不变量。

### 3. 撮合：价格优先、时间优先

买卖两个订单簿用 `TreeMap` 维护：买盘价高者优先、卖盘价低者优先，同价则定序 ID 小者（先到）优先。
taker 订单不断与对手盘最优订单以 **maker 挂单价**成交，撮不尽的剩余挂入自己的订单簿。
买方成交价低于挂单价时，多冻结的差额在清算时退回。

### 4. 定序保证全局有序

所有写事件先进入定序服务，被赋予**全局唯一且严格递增**的 `sequenceId`（并记录 `previousId`），
持久化后再进入交易引擎。这让分布式系统获得一条**唯一有序的事件流**，是确定性的前提。

### 5. Spring Cloud Config 集中配置

Config Server 以 native 模式从 `config-repo/` 读取配置。每个应用启动时按
`{name}-{profile}.yml → application-{profile}.yml → {name}.yml → application.yml` 的优先级合并配置。
敏感信息（如数据库口令）通过**应用自身的环境变量**注入，Config Server 不做替换。

---

## 技术栈

- **Java 17**，**Spring Boot 3.2**，**Spring Cloud 2023.0**
- Spring Cloud Config（配置中心）
- Apache Kafka（事件流：sequence / trade / tick 三个主题）
- Redis（订单簿快照、行情缓存、推送 Pub/Sub）
- MySQL 8 + Spring Data JPA（订单、成交、K 线、用户等持久化）
- Spring WebSocket（实时推送）
- Pebble（服务端模板）
- Maven 多模块 · JUnit 5

> 说明：教程原文基于 Spring Boot 3.0 / Java 17，本项目升级到 Spring Boot 3.2 以兼容 JDK 21 构建；
> 中间件用官方 `confluentinc/cp-kafka` + `redis` + `mysql` 镜像，行为一致。

---

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.9+
- Docker（用于运行 Kafka / Redis / MySQL）

### 1. 启动基础中间件

```bash
docker compose up -d          # 启动 Kafka(+ZooKeeper)、Redis、MySQL(自动建表)
```

### 2. 打包所有模块

```bash
cd build && mvn clean package  # 编译、测试、打出各服务可执行 jar
cd ..
```

### 3. 启动所有微服务

```bash
scripts/run-all.sh             # 先起 config(8888)，再起其余服务；日志在 logs/
```

（也可在 IDE 中分别运行各 `*Application`，务必先启动 `ConfigApplication`。）

### 4. 打开 Web 界面

浏览器访问 **http://localhost:8000** ：注册 → 登录 → 用「Deposit(demo)」充值 →
下单买卖，即可看到订单簿、资产与活动订单实时变化。

### 停止

```bash
scripts/stop-all.sh
docker compose down            # 加 -v 可清空数据卷
```

---

## REST API

认证：登录返回的 `token`（演示中即 userId）通过 `Authorization: Bearer <token>` 携带。

| 方法 | 路径 | 认证 | 说明 |
|------|------|:----:|------|
| POST | `/api/auth/signup` | ✗ | 注册 `{email,password,name}` |
| POST | `/api/auth/signin` | ✗ | 登录，返回 `{userId, token}` |
| GET  | `/api/orderBook` | ✗ | 订单簿快照 |
| GET  | `/api/marketPrice` | ✗ | 最新市场价 |
| GET  | `/api/assets` | ✓ | 当前用户资产 |
| GET  | `/api/orders` | ✓ | 当前用户活动订单 |
| GET  | `/api/orders/{id}` | ✓ | 单个订单 |
| POST | `/api/orders` | ✓ | 下单 `{direction,price,quantity}` |
| POST | `/api/orders/{id}/cancel` | ✓ | 撤单 |
| POST | `/api/transfer` | ✓ | 充值(演示) `{asset,amount}` |
| GET  | `/api/history/orders` | ✓ | 历史订单 |
| GET  | `/api/quotation/bars/{sec|min|hour|day}` | ✗ | K 线（quotation:8004） |

---

## 测试

交易引擎的核心逻辑有完整单元测试，**不依赖任何中间件**即可运行：

```bash
mvn -f trading-engine/pom.xml test
```

- `AssetServiceTest` —— 各类转账/冻结/解冻，并校验**系统资产守恒**（总额恒为 0）
- `MatchEngineTest` —— 价格优先/时间优先、跨档撮合、以 maker 价成交、订单簿聚合
- `TradingEngineServiceTest` —— 端到端：充值 → 下单 → 撮合 → 清算 → 撤单，校验资产变化与守恒
- `MessagingRoundTripIntegrationTest` —— 用**内嵌 Kafka**（纯 JVM，无需 Docker）验证完整消息链路：
  事件 JSON 序列化 → Kafka → 消费 → 按 `@class` 多态反序列化回具体子类型 → 引擎处理，结果正确且守恒

---

## 面试要点

- **为什么全内存？** 数据库事务撮合 TPS 太低；全内存撮合 + 事件溯源持久化，兼顾性能与可靠。
- **宕机怎么办？** 引擎是确定性状态机，重启后从定序事件流重放即可恢复到一致状态；多实例并行消费同一事件流实现热备。
- **如何保证不重不漏？** 定序服务赋予全局有序 `sequenceId` + `previousId`，下游据此校验连续性。
- **如何对账？** 引入系统负债账户，保证资产负债表恒为零，任意时刻可校验。
- **精度问题？** 金额一律用 `BigDecimal`，数据库用 `DECIMAL(36,18)`。
- **模块解耦？** 通过 Kafka（写链路）与 Redis（读快照/推送）解耦，API 只是薄薄的入口层。
