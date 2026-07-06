package com.itranswarp.exchange.enums;

/**
 * 撮合类型，用于标识一条成交明细中该订单扮演的角色。
 */
public enum MatchType {

    /**
     * 挂单方 (Maker)：先进入订单簿提供流动性的一方。
     */
    TAKER,

    /**
     * 吃单方 (Taker)：主动与订单簿成交的一方。
     */
    MAKER;
}
