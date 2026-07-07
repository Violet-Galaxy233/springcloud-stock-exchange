package com.itranswarp.exchange.tradingapi.web;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.itranswarp.exchange.ctx.UserContext;
import com.itranswarp.exchange.tradingapi.user.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证过滤器：从 {@code Authorization: Bearer <jwt>} 解析并校验 JWT，
 * 取出用户 ID 后通过 {@link UserContext} 绑定到当前线程，供后续业务读取。
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public AuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

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
        return this.jwtService.verifyToken(token);
    }
}
