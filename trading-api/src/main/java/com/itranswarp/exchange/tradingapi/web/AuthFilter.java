package com.itranswarp.exchange.tradingapi.web;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.itranswarp.exchange.ctx.UserContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证过滤器：从 {@code Authorization: Bearer <token>} 解析 token (演示中即用户 ID)，
 * 通过 {@link UserContext} 绑定到当前线程，供后续业务读取。
 * <p>
 * 演示用的简单方案；生产环境应使用签名的 JWT 或不透明 token + 会话存储。
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long userId = parseToken(request.getHeader("Authorization"));
        if (userId != null) {
            try (UserContext ctx = new UserContext(userId)) {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private Long parseToken(String header) {
        if (header == null) {
            return null;
        }
        String token = header.startsWith("Bearer ") ? header.substring(7).strip() : header.strip();
        if (token.isEmpty()) {
            return null;
        }
        try {
            long userId = Long.parseLong(token);
            return userId > 0 ? userId : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
