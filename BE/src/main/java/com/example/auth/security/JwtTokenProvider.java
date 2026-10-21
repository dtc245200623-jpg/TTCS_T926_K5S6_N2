package com.example.auth.security;

import com.example.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Khóa bí mật tối thiểu 256 bits (32 bytes) cho thuật toán HS256
    @Value("${jwt.secret:mySecretKeyForJwtSigningMustBeVeryLongAndSecure12345678}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}") // 24 giờ
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Tạo token JWT bao gồm username và token_version
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("token_version", user.getTokenVersion()) // Đính kèm token_version vào payload
                .claim("roles", user.getRoles())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Trích xuất username từ JWT token
     */
    public String getUsernameFromToken(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Trích xuất token_version được mã hóa trong JWT token
     */
    public Long getTokenVersionFromToken(String token) {
        Claims claims = extractClaims(token);
        Object version = claims.get("token_version");
        if (version instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Trích xuất Claims từ JWT
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Xác thực cấu trúc và chữ ký hợp lệ của JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
