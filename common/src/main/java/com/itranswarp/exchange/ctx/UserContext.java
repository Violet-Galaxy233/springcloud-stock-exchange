package com.itranswarp.exchange.ctx;

import org.springframework.lang.Nullable;

import com.itranswarp.exchange.ApiError;
import com.itranswarp.exchange.ApiException;

/**
 * 基于 ThreadLocal 保存当前请求的用户 ID，配合 try-with-resources 自动清理。
 *
 * <pre>
 * try (var ctx = new UserContext(userId)) {
 *     // 业务逻辑期间 UserContext.getRequiredUserId() 可取到 userId
 * }
 * </pre>
 */
public class UserContext implements AutoCloseable {

    private static final ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();

    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }

    /**
     * 获取当前用户 ID，未登录返回 null。
     */
    @Nullable
    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }

    /**
     * 获取当前用户 ID，未登录抛出 {@link ApiException}。
     */
    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ApiException(ApiError.AUTH_FORBIDDEN, null, "Need signin.");
        }
        return userId;
    }

    @Override
    public void close() {
        THREAD_LOCAL_CTX.remove();
    }
}
