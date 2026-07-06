package com.itranswarp.exchange.enums;

/**
 * 订单方向：买入或卖出。
 */
public enum Direction {

    /**
     * 买入 (花费 USD 买入 BTC)。
     */
    BUY(1),

    /**
     * 卖出 (卖出 BTC 获得 USD)。
     */
    SELL(0);

    /**
     * 用整型表示方向，便于持久化存储。
     */
    public final int value;

    Direction(int value) {
        this.value = value;
    }

    /**
     * 返回相反的方向。
     */
    public Direction negate() {
        return this == BUY ? SELL : BUY;
    }

    public static Direction of(int intValue) {
        if (intValue == 1) {
            return BUY;
        }
        if (intValue == 0) {
            return SELL;
        }
        throw new IllegalArgumentException("Invalid direction value: " + intValue);
    }
}
