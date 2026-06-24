package com.campus.exhibition.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成、解析、校验 Token
 */
@Component
public class JwtUtils {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Long userId, String role, Long collegeId) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key);

        if (collegeId != null) {
            builder.claim("collegeId", collegeId);
        }
        return builder.compact();
    }

    /**
     * 解析 Token 中的 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 Token 中提取 userId
     */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }
}
