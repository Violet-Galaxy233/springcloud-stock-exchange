package com.itranswarp.exchange.enums;

/**
 * 资产类型。
 * <p>
 * 本项目仅支持 BTC/USD 一个交易对，因此只有 USD 与 BTC 两种资产。
 * 若要扩展更多资产，可将资产 ID 改为整型，并维护资产 ID -> 名称的映射。
 */
public enum AssetEnum {

    /**
     * 美元，计价货币 (Quote Asset)。
     */
    USD,

    /**
     * 比特币，交易资产 (Base Asset)。
     */
    BTC;
}
