package com.itranswarp.exchange.message.event;

import java.math.BigDecimal;

import com.itranswarp.exchange.enums.AssetEnum;

/**
 * 资产转账/充值事件。
 * <p>
 * 用户充值时由系统负债账户向用户账户转入资产，此时 {@link #sufficient} 为 false，
 * 表示无需检查转出方 (负债账户) 的余额。
 */
public class TransferEvent extends AbstractEvent {

    public Long fromUserId;
    public Long toUserId;
    public AssetEnum asset;
    public BigDecimal amount;

    /**
     * 是否需要检查转出方余额充足。
     */
    public boolean sufficient;

    @Override
    public String toString() {
        return "TransferEvent [sequenceId=" + sequenceId + ", fromUserId=" + fromUserId + ", toUserId=" + toUserId
                + ", asset=" + asset + ", amount=" + amount + ", sufficient=" + sufficient + "]";
    }
}
