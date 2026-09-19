package com.travelbird.global.security;

import com.travelbird.global.security.TokenType;
import com.travelbird.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String createAccessToken(Long userId, UserRole role) {
        return createToken(userId, role, TokenType.ACCESS, accessTokenExpirationSeconds);
    }

    public String createRefreshToken(Long userId, UserRole role) {
        return createToken(userId, role, TokenType.REFRESH, refreshTokenExpirationSeconds);
    }

    private String createToken(Long userId, UserRole role, TokenType tokenType, long expirationSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim("userId", userId)
                .claim("role", role.name())
                .claim("tokenType", tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }
}
