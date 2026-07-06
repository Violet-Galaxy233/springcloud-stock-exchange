package com.itranswarp.exchange.redis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.itranswarp.exchange.support.LoggerSupport;

/**
 * 对 Redis 的常用操作进行封装：读写、执行 Lua 脚本、发布/订阅。
 */
public class RedisService extends LoggerSupport {

    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisService(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public List<String> lrange(String key, long start, long end) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    public List<String> zrangebyscore(String key, long start, long end) {
        var set = stringRedisTemplate.opsForZSet().rangeByScore(key, start, end);
        return set == null ? List.of() : List.copyOf(set);
    }

    /**
     * 执行返回 Long 的 Lua 脚本 (常用于定序号自增等原子操作)。
     */
    public Long executeScriptReturnLong(String scriptText, String[] keys, Object[] args) {
        RedisScript<Long> script = RedisScript.of(scriptText, Long.class);
        return stringRedisTemplate.execute(script, List.of(keys), args);
    }

    /**
     * 执行返回 String 的 Lua 脚本。
     */
    public String executeScriptReturnString(String scriptText, String[] keys, Object[] args) {
        RedisScript<String> script = RedisScript.of(scriptText, String.class);
        return stringRedisTemplate.execute(script, List.of(keys), args);
    }

    /**
     * 发布消息到指定频道。
     */
    public void publish(String topic, String data) {
        stringRedisTemplate.convertAndSend(topic, data);
    }

    /**
     * 订阅指定频道，收到消息后回调 listener。返回可关闭的监听容器。
     */
    public RedisMessageListenerContainer subscribe(String topic, Consumer<String> listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(this.redisConnectionFactory);
        container.addMessageListener((message, pattern) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            listener.accept(body);
        }, new ChannelTopic(topic));
        container.afterPropertiesSet();
        container.start();
        logger.info("subscribe to redis topic {}...", topic);
        return container;
    }
}
