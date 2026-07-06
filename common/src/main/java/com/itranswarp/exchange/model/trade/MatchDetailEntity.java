package com.itranswarp.exchange.model.trade;

import java.math.BigDecimal;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.MatchType;
import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 成交明细实体。撮合成功产生一条 taker 记录和一条对应的 maker 记录。
 */
@Entity
@Table(name = "match_details")
public class MatchDetailEntity implements EntitySupport, Comparable<MatchDetailEntity> {

    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * 定序 ID (来自触发本次撮合的 taker 订单)。
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long orderId;

    @Column(nullable = false, updatable = false)
    public Long counterOrderId;

    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(nullable = false, updatable = false)
    public Long counterUserId;

    /**
     * 本条明细中该订单的角色 (TAKER / MAKER)。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    public MatchType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    public Direction direction;

    /**
     * 成交价格 (以 maker 挂单价成交)。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal price;

    /**
     * 成交数量。
     */
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

    @Override
    public int compareTo(MatchDetailEntity o) {
        return Long.compare(this.id, o.id);
    }

    @Override
    public String toString() {
        return "MatchDetailEntity [id=" + id + ", sequenceId=" + sequenceId + ", orderId=" + orderId
                + ", counterOrderId=" + counterOrderId + ", type=" + type + ", direction=" + direction + ", price="
                + price + ", quantity=" + quantity + "]";
    }
}
