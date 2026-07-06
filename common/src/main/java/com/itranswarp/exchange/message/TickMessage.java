package com.itranswarp.exchange.message;

import java.util.List;

import com.itranswarp.exchange.model.quotation.TickEntity;

/**
 * 交易引擎撮合成交后，将一批 Tick 发送给行情系统聚合 K 线。
 */
public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;
}
