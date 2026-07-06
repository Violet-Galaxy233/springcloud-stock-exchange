package com.itranswarp.exchange.tradingengine.assets;

/**
 * 资产转账类型。所有资产操作本质上都是转账。
 */
public enum Transfer {

    /**
     * 可用转可用。
     */
    AVAILABLE_TO_AVAILABLE,

    /**
     * 可用转冻结 (下单时冻结资产)。
     */
    AVAILABLE_TO_FROZEN,

    /**
     * 冻结转可用 (撤单时解冻资产)。
     */
    FROZEN_TO_AVAILABLE;
}
