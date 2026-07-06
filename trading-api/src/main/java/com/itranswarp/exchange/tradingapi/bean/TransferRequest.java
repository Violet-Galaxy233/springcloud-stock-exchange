package com.itranswarp.exchange.tradingapi.bean;

import java.math.BigDecimal;

import com.itranswarp.exchange.enums.AssetEnum;

/**
 * 充值 (演示) 请求体。
 */
public class TransferRequest {

    public AssetEnum asset;
    public BigDecimal amount;
}
