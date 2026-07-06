package com.itranswarp.exchange.redis;

/**
 * Redis 中使用的 Key 与频道名称常量。
 */
public interface RedisCache {

    /**
     * 推送通知频道 (Pub/Sub)：交易引擎发布，推送服务订阅。
     */
    String Topic = "notification";

    /**
     * 订单簿快照 Key，交易引擎实时写入，供 API/UI 读取。
     */
    String OrderBook = "_orderbook_";

    /**
     * 最近成交 Tick 列表 Key。
     */
    String RecentTicks = "_recent_ticks_";

    /**
     * 各粒度 K 线在 Redis 中的 ZSet Key。
     */
    String DayBars = "_day_bars_";
    String HourBars = "_hour_bars_";
    String MinBars = "_min_bars_";
    String SecBars = "_sec_bars_";
}
