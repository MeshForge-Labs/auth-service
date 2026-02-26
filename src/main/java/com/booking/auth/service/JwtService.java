package com.booking.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;
    private static final String ROLES_CLAIM = "roles";

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:900000}") long expirationMs,
            @Value("${jwt.issuer:auth-service}") String issuer) {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be set and at least 32 characters for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateToken(String subject, Set<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(subject)
                .claim(ROLES_CLAIM, roles)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public java.util.Optional<Claims> validateAndParse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return java.util.Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return java.util.Optional.empty();
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromClaims(Claims claims) {
        Object roles = claims.get(ROLES_CLAIM);
        if (roles instanceof Iterable<?> iterable) {
            Set<String> set = new java.util.HashSet<>();
            for (Object r : iterable) {
                if (r != null) set.add(r.toString());
            }
            return set;
        }
        return Collections.emptySet();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
