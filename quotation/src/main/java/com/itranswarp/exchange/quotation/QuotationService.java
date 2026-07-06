package com.itranswarp.exchange.quotation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.message.TickMessage;
import com.itranswarp.exchange.messaging.MessageConsumer;
import com.itranswarp.exchange.messaging.Messaging;
import com.itranswarp.exchange.messaging.MessagingFactory;
import com.itranswarp.exchange.model.quotation.AbstractBarEntity;
import com.itranswarp.exchange.model.quotation.DayBarEntity;
import com.itranswarp.exchange.model.quotation.HourBarEntity;
import com.itranswarp.exchange.model.quotation.MinBarEntity;
import com.itranswarp.exchange.model.quotation.SecBarEntity;
import com.itranswarp.exchange.model.quotation.TickEntity;
import com.itranswarp.exchange.quotation.db.DayBarRepository;
import com.itranswarp.exchange.quotation.db.HourBarRepository;
import com.itranswarp.exchange.quotation.db.MinBarRepository;
import com.itranswarp.exchange.quotation.db.SecBarRepository;
import com.itranswarp.exchange.redis.RedisCache;
import com.itranswarp.exchange.redis.RedisService;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.util.JsonUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 行情核心服务。
 * <p>
 * 消费 TICK 主题中的成交 Tick，实时聚合为秒/分/时/日 4 种粒度的 K 线：
 * 相同起始时间的 Tick 合并进"当前" Bar，跨到新的时间窗口时将已完成的 Bar 持久化并开启新 Bar。
 */
@Component
public class QuotationService extends LoggerSupport {

    // 各粒度时间窗口 (毫秒):
    private static final long SEC_INTERVAL = 1000L;
    private static final long MIN_INTERVAL = 60_000L;
    private static final long HOUR_INTERVAL = 3600_000L;
    private static final long DAY_INTERVAL = 86_400_000L;

    // 内存中每种粒度的"当前" Bar (尚未完成，随新 Tick 不断合并):
    private final BarBuilder<SecBarEntity> secBuilder;
    private final BarBuilder<MinBarEntity> minBuilder;
    private final BarBuilder<HourBarEntity> hourBuilder;
    private final BarBuilder<DayBarEntity> dayBuilder;

    @Autowired
    private MessagingFactory messagingFactory;

    @Autowired
    private RedisService redisService;

    private MessageConsumer consumer;

    public QuotationService(@Autowired SecBarRepository secBarRepository,
            @Autowired MinBarRepository minBarRepository, @Autowired HourBarRepository hourBarRepository,
            @Autowired DayBarRepository dayBarRepository) {
        this.secBuilder = new BarBuilder<>(SEC_INTERVAL, SecBarEntity::new, secBarRepository, RedisCache.SecBars);
        this.minBuilder = new BarBuilder<>(MIN_INTERVAL, MinBarEntity::new, minBarRepository, RedisCache.MinBars);
        this.hourBuilder = new BarBuilder<>(HOUR_INTERVAL, HourBarEntity::new, hourBarRepository, RedisCache.HourBars);
        this.dayBuilder = new BarBuilder<>(DAY_INTERVAL, DayBarEntity::new, dayBarRepository, RedisCache.DayBars);
    }

    /**
     * 启动时创建 TICK 主题的批量消费者。
     */
    @PostConstruct
    public void init() {
        this.consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TICK, "quotation",
                this::processMessages);
    }

    /**
     * 停止消费者。
     */
    @PreDestroy
    public void shutdown() {
        if (this.consumer != null) {
            this.consumer.stop();
        }
    }

    /**
     * 批量处理 TickMessage：逐条 Tick 更新各粒度 K 线并刷新最近成交缓存。
     */
    public void processMessages(List<TickMessage> messages) {
        for (TickMessage message : messages) {
            processTicks(message.ticks);
        }
    }

    private void processTicks(List<TickEntity> ticks) {
        if (ticks == null || ticks.isEmpty()) {
            return;
        }
        for (TickEntity tick : ticks) {
            this.secBuilder.update(tick);
            this.minBuilder.update(tick);
            this.hourBuilder.update(tick);
            this.dayBuilder.update(tick);
        }
        // 将本批最近成交写入 Redis (存最新的一批 Tick 的 JSON 列表):
        try {
            List<String> jsonList = new ArrayList<>(ticks.size());
            for (TickEntity tick : ticks) {
                jsonList.add(tick.toJson());
            }
            this.redisService.set(RedisCache.RecentTicks, JsonUtil.writeJson(jsonList));
        } catch (Exception e) {
            logger.warn("update recent ticks to redis failed.", e);
        }
    }

    /**
     * 单一粒度的 K 线构建器：维护当前未完成的 Bar，跨窗口时持久化并开启新 Bar。
     */
    private class BarBuilder<T extends AbstractBarEntity> {

        private final long interval;
        private final Supplier<T> factory;
        private final CrudRepository<T, Long> repository;
        private final String redisKey;

        // 当前正在累积的 Bar，null 表示尚无数据:
        private T current;

        BarBuilder(long interval, Supplier<T> factory, CrudRepository<T, Long> repository, String redisKey) {
            this.interval = interval;
            this.factory = factory;
            this.repository = repository;
            this.redisKey = redisKey;
        }

        /**
         * 用一条 Tick 更新当前 Bar。
         */
        void update(TickEntity tick) {
            // 将成交时间向下取整到本粒度的窗口起始:
            long startTime = tick.createdAt - tick.createdAt % this.interval;
            if (this.current == null) {
                this.current = newBar(startTime, tick);
                return;
            }
            if (startTime == this.current.startTime) {
                // 同一窗口，合并:
                merge(this.current, tick);
            } else {
                // 跨到新窗口：先固化旧 Bar，再开启新 Bar:
                finish(this.current);
                this.current = newBar(startTime, tick);
            }
        }

        private T newBar(long startTime, TickEntity tick) {
            T bar = this.factory.get();
            bar.startTime = startTime;
            bar.openPrice = tick.price;
            bar.highPrice = tick.price;
            bar.lowPrice = tick.price;
            bar.closePrice = tick.price;
            bar.quantity = tick.quantity;
            return bar;
        }

        private void merge(T bar, TickEntity tick) {
            bar.highPrice = bar.highPrice.max(tick.price);
            bar.lowPrice = bar.lowPrice.min(tick.price);
            bar.closePrice = tick.price;
            bar.quantity = bar.quantity.add(tick.quantity);
        }

        /**
         * 固化一个已完成的 Bar：持久化到数据库，并写入 Redis (尽力而为)。
         */
        private void finish(T bar) {
            try {
                this.repository.save(bar);
            } catch (Exception e) {
                logger.warn("save bar to db failed: {}", bar, e);
            }
            try {
                BigDecimal[] arr = bar.toBarArray();
                QuotationService.this.redisService.set(this.redisKey + bar.startTime, JsonUtil.writeJson(arr));
            } catch (Exception e) {
                logger.warn("update bar to redis failed: {}", bar, e);
            }
        }
    }
}
