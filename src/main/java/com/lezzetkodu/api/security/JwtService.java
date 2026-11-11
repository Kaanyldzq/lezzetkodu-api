package com.lezzetkodu.api.security;

import com.lezzetkodu.api.user.Role;
import com.lezzetkodu.api.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * JWT = sunucunun imzaladigi kimlik karti. Icinde kullanici id'si, adi ve rolu var.
 * Sunucu her istekte imzayi dogrular; oturum bilgisi saklamaz (stateless).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            // Gizli anahtar yoksa uygulama hic acilmasin: varsayilan/sabit anahtarla calismak guvenlik acigidir.
            throw new IllegalStateException("JWT_SECRET ortam değişkeni tanımlı değil. Üretmek için: openssl rand -base64 32");
        }
        byte[] bytes = Decoders.BASE64.decode(properties.secret());
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET en az 256 bit (32 bayt) olmalı");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiration = properties.expiration();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Imza gecersizse, suresi dolmussa veya bicim bozuksa bos doner. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    Role.valueOf(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expiresInSeconds() {
        return expiration.toSeconds();
    }
}
