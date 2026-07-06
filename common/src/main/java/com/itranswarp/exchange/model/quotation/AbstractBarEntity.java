package com.itranswarp.exchange.model.quotation;

import java.math.BigDecimal;

import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * K 线 (Bar) 的抽象基类，包含 OHLCV 五个要素。
 */
@MappedSuperclass
public abstract class AbstractBarEntity implements EntitySupport {

    /**
     * 该 Bar 的开始时间 (毫秒)，作为主键。
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long startTime;

    /**
     * 开盘价。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal openPrice;

    /**
     * 最高价。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal highPrice;

    /**
     * 最低价。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal lowPrice;

    /**
     * 收盘价。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal closePrice;

    /**
     * 成交量。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal quantity;

    @Override
    public long getCreatedAt() {
        return this.startTime;
    }

    @Override
    public long getUpdatedAt() {
        return this.startTime;
    }

    /**
     * 序列化为 K 线数组: [开始时间, 开, 高, 低, 收, 量]。
     */
    public BigDecimal[] toBarArray() {
        return new BigDecimal[] { BigDecimal.valueOf(this.startTime), this.openPrice, this.highPrice, this.lowPrice,
                this.closePrice, this.quantity };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [startTime=" + startTime + ", O=" + openPrice + ", H=" + highPrice
                + ", L=" + lowPrice + ", C=" + closePrice + ", V=" + quantity + "]";
    }
}
