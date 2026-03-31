package com.example.order.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Generates JWT tokens for API tests.
 * Uses the same secret as application-test.yml to pass Spring Security.
 */
public final class JwtTestHelper {

    // Must match: app.security.jwt.secret in application-test.yml
    private static final String TEST_SECRET =
        "test-secret-key-minimum-32-chars-long-for-hmac";

    private static final SecretKey KEY =
        Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

    private JwtTestHelper() {}

    public static String validToken(String userId, String... roles) {
        return Jwts.builder()
            .subject(userId)
            .claim("roles", List.of(roles))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(KEY)
            .compact();
    }

    public static String expiredToken(String userId) {
        return Jwts.builder()
            .subject(userId)
            .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
            .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
            .signWith(KEY)
            .compact();
    }

    public static String tokenForUser(String userId) {
        return validToken(userId, "ROLE_USER");
    }

    public static String adminToken() {
        return validToken("admin-1", "ROLE_ADMIN");
    }
}