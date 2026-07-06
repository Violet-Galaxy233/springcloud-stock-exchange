package com.itranswarp.exchange.model.quotation;

import java.math.BigDecimal;

import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 一条成交 Tick。撮合每产生一笔成交即生成一条 Tick，行情系统据此聚合 K 线。
 */
@Entity
@Table(name = "ticks")
public class TickEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * taker 方向：true 表示 taker 是买方 (主动买)。
     */
    @Column(nullable = false, updatable = false)
    public boolean takerDirection;

    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal price;

    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal quantity;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public long getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public long getUpdatedAt() {
        return this.createdAt;
    }

    /**
     * 序列化为紧凑的 JSON 数组字符串，用于推送与缓存: [时间, 方向, 价格, 数量]。
     */
    public String toJson() {
        return "[" + this.createdAt + "," + (this.takerDirection ? 1 : 0) + "," + this.price + "," + this.quantity
                + "]";
    }

    @Override
    public String toString() {
        return "TickEntity [id=" + id + ", sequenceId=" + sequenceId + ", takerDirection=" + takerDirection + ", price="
                + price + ", quantity=" + quantity + ", createdAt=" + createdAt + "]";
    }
}
