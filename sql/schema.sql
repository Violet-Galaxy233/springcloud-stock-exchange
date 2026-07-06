-- Warp Exchange 数据库表结构
-- MySQL 首次启动时通过 docker-compose 挂载自动执行。

CREATE DATABASE IF NOT EXISTS exchange DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE exchange;

-- 用户表 --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    type      VARCHAR(32) NOT NULL,
    createdAt BIGINT      NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 用户资料 ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_profiles (
    userId    BIGINT       NOT NULL,
    email     VARCHAR(100) NOT NULL,
    name      VARCHAR(100) NOT NULL,
    createdAt BIGINT       NOT NULL,
    updatedAt BIGINT       NOT NULL,
    PRIMARY KEY (userId),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB;

-- 口令认证 ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_auths (
    userId    BIGINT       NOT NULL,
    random    VARCHAR(100) NOT NULL,
    passwd    VARCHAR(64)  NOT NULL,
    createdAt BIGINT       NOT NULL,
    PRIMARY KEY (userId)
) ENGINE=InnoDB;

-- 订单表 (交易引擎持久化) --------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT        NOT NULL,
    sequenceId       BIGINT        NOT NULL,
    userId           BIGINT        NOT NULL,
    price            DECIMAL(36,18) NOT NULL,
    direction        VARCHAR(32)   NOT NULL,
    status           VARCHAR(32)   NOT NULL,
    quantity         DECIMAL(36,18) NOT NULL,
    unfilledQuantity DECIMAL(36,18) NOT NULL,
    createdAt        BIGINT        NOT NULL,
    updatedAt        BIGINT        NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user (userId)
) ENGINE=InnoDB;

-- 成交明细 ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS match_details (
    id             BIGINT         NOT NULL,
    sequenceId     BIGINT         NOT NULL,
    orderId        BIGINT         NOT NULL,
    counterOrderId BIGINT         NOT NULL,
    userId         BIGINT         NOT NULL,
    counterUserId  BIGINT         NOT NULL,
    type           VARCHAR(32)    NOT NULL,
    direction      VARCHAR(32)    NOT NULL,
    price          DECIMAL(36,18) NOT NULL,
    quantity       DECIMAL(36,18) NOT NULL,
    createdAt      BIGINT         NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order (orderId),
    KEY idx_user (userId)
) ENGINE=InnoDB;

-- 成交 Tick ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticks (
    id             BIGINT         NOT NULL,
    sequenceId     BIGINT         NOT NULL,
    takerDirection BIT            NOT NULL,
    price          DECIMAL(36,18) NOT NULL,
    quantity       DECIMAL(36,18) NOT NULL,
    createdAt      BIGINT         NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 定序事件 (定序服务持久化，用于故障恢复重放) ------------------------
CREATE TABLE IF NOT EXISTS events (
    sequenceId BIGINT         NOT NULL,
    previousId BIGINT         NOT NULL,
    data       VARCHAR(10000) NOT NULL,
    createdAt  BIGINT         NOT NULL,
    PRIMARY KEY (sequenceId)
) ENGINE=InnoDB;

-- K 线 (各时间粒度) ---------------------------------------------------
CREATE TABLE IF NOT EXISTS sec_bars (
    startTime  BIGINT         NOT NULL,
    openPrice  DECIMAL(36,18) NOT NULL,
    highPrice  DECIMAL(36,18) NOT NULL,
    lowPrice   DECIMAL(36,18) NOT NULL,
    closePrice DECIMAL(36,18) NOT NULL,
    quantity   DECIMAL(36,18) NOT NULL,
    PRIMARY KEY (startTime)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS min_bars (
    startTime  BIGINT         NOT NULL,
    openPrice  DECIMAL(36,18) NOT NULL,
    highPrice  DECIMAL(36,18) NOT NULL,
    lowPrice   DECIMAL(36,18) NOT NULL,
    closePrice DECIMAL(36,18) NOT NULL,
    quantity   DECIMAL(36,18) NOT NULL,
    PRIMARY KEY (startTime)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS hour_bars (
    startTime  BIGINT         NOT NULL,
    openPrice  DECIMAL(36,18) NOT NULL,
    highPrice  DECIMAL(36,18) NOT NULL,
    lowPrice   DECIMAL(36,18) NOT NULL,
    closePrice DECIMAL(36,18) NOT NULL,
    quantity   DECIMAL(36,18) NOT NULL,
    PRIMARY KEY (startTime)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS day_bars (
    startTime  BIGINT         NOT NULL,
    openPrice  DECIMAL(36,18) NOT NULL,
    highPrice  DECIMAL(36,18) NOT NULL,
    lowPrice   DECIMAL(36,18) NOT NULL,
    closePrice DECIMAL(36,18) NOT NULL,
    quantity   DECIMAL(36,18) NOT NULL,
    PRIMARY KEY (startTime)
) ENGINE=InnoDB;

-- 系统负债账户 (ID = 1)：所有用户权益之和记入该账户，保证系统资产负债表恒为零。
INSERT INTO users (id, type, createdAt) VALUES (1, 'DEBT', 0)
    ON DUPLICATE KEY UPDATE id = id;
