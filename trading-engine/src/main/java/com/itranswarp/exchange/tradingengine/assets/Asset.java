package com.itranswarp.exchange.tradingengine.assets;

import java.math.BigDecimal;

/**
 * 用户某一种资产的余额，包含可用余额与冻结余额。
 */
public class Asset {

    /**
     * 可用余额。
     */
    BigDecimal available;

    /**
     * 冻结余额。
     */
    BigDecimal frozen;

    public Asset() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getFrozen() {
        return frozen;
    }

    @Override
    public String toString() {
        return "[available=" + available + ", frozen=" + frozen + "]";
    }
}
