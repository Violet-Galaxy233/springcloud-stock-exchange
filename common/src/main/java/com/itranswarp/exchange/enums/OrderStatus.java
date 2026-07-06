package com.itranswarp.exchange.enums;

/**
 * 订单状态。
 */
public enum OrderStatus {

    /**
     * 等待成交。
     */
    PENDING(false),

    /**
     * 完全成交 (结束状态)。
     */
    FULLY_FILLED(true),

    /**
     * 部分成交。
     */
    PARTIAL_FILLED(false),

    /**
     * 部分成交后被取消 (结束状态)。
     */
    PARTIAL_CANCELLED(true),

    /**
     * 完全取消 (结束状态)。
     */
    FULLY_CANCELLED(true);

    /**
     * 是否为最终状态。处于最终状态的订单会从活动订单中移除。
     */
    public final boolean isFinalStatus;

    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }
}
