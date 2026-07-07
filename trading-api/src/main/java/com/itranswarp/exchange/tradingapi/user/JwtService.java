package com.itranswarp.exchange.tradingapi.user;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.support.LoggerSupport;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 签发与校验服务，替代早期演示用的"token 即 userId"方案。
 * <p>
 * 使用 HS256 对称签名，密钥与有效期通过配置注入。token 的 subject 为用户 ID。
 */
@Component
public class JwtService extends LoggerSupport {

    private final SecretKey secretKey;
    private final long expiresInMillis;

    public JwtService(@Value("${exchange.jwt.secret:warp-exchange-please-change-this-secret-key-32bytes+}") String secret,
            @Value("${exchange.jwt.expires-in-seconds:86400}") long expiresInSeconds) {
        // HS256 要求密钥至少 256 位 (32 字节):
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("exchange.jwt.secret must be at least 32 bytes for HS256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiresInMillis = expiresInSeconds * 1000L;
    }

    /**
     * 为用户签发 JWT。
     */
    public String createToken(Long userId, String name) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("name", name)
                .issuedAt(new Date(now))
                .expiration(new Date(now + this.expiresInMillis))
                .signWith(this.secretKey)
                .compact();
    }

    /**
     * 校验 token 并返回用户 ID；无效或过期返回 null。
     */
    @Nullable
    public Long verifyToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(this.secretKey).build().parseSignedClaims(token);
            return Long.valueOf(jws.getPayload().getSubject());
        } catch (JwtException | NumberFormatException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("invalid jwt: {}", e.getMessage());
            }
            return null;
        }
    }
}
