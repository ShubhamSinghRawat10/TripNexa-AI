package com.tripnexa.smarttrip.security;

import com.tripnexa.smarttrip.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String generate(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(principal.email())
            .claim("uid", principal.id().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.getExpiration())))
            .signWith(signingKey)
            .compact();
    }

    public String extractEmail(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token, UserPrincipal principal) {
        Claims claims = claims(token);
        return principal.email().equalsIgnoreCase(claims.getSubject())
            && claims.getExpiration().after(new Date());
    }

    public long expirationSeconds() {
        return properties.getExpiration().toSeconds();
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
            .parseSignedClaims(token).getPayload();
    }
}
