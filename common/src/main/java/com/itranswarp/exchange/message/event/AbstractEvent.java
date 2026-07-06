package com.itranswarp.exchange.message.event;

import org.springframework.lang.Nullable;

import com.itranswarp.exchange.message.AbstractMessage;

/**
 * 定序事件基类。
 * <p>
 * 定序服务 (Sequencer) 会为每个进入交易引擎的事件分配全局唯一且递增的
 * {@link #sequenceId}，并记录 {@link #previousId}，交易引擎据此保证事件严格有序、
 * 不重不漏。
 */
public class AbstractEvent extends AbstractMessage {

    /**
     * 定序 ID (全局唯一递增)。
     */
    public long sequenceId;

    /**
     * 上一个事件的定序 ID，用于检测消息是否连续。
     */
    public long previousId;

    /**
     * 幂等去重 ID，可为空。相同 uniqueId 的事件只会被处理一次。
     */
    @Nullable
    public String uniqueId;
}
