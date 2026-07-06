package com.itranswarp.exchange.push;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.message.NotificationMessage;
import com.itranswarp.exchange.redis.RedisCache;
import com.itranswarp.exchange.redis.RedisService;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.util.JsonUtil;

/**
 * 推送服务。订阅 Redis 通知频道，将收到的消息分发给对应用户或广播。
 */
@Component
public class PushService extends LoggerSupport {

    private final RedisService redisService;
    private final NotificationHandler notificationHandler;

    private RedisMessageListenerContainer container;

    public PushService(RedisService redisService, NotificationHandler notificationHandler) {
        this.redisService = redisService;
        this.notificationHandler = notificationHandler;
    }

    @PostConstruct
    public void init() {
        logger.info("subscribe to redis topic {}...", RedisCache.Topic);
        this.container = redisService.subscribe(RedisCache.Topic, this::onMessage);
    }

    @PreDestroy
    public void shutdown() {
        if (this.container != null) {
            this.container.stop();
            this.container = null;
        }
    }

    /**
     * 收到通知消息：有 userId 则定向推送，否则广播。
     */
    public void onMessage(String json) {
        if (logger.isDebugEnabled()) {
            logger.debug("received notification message: {}", json);
        }
        NotificationMessage message = JsonUtil.readJson(json, NotificationMessage.class);
        if (message.userId != null) {
            notificationHandler.sendToUser(message.userId, json);
        } else {
            notificationHandler.broadcast(json);
        }
    }
}
