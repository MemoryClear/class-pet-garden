package com.classpet.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, String teacherId) {
        return generateToken(username, teacherId, "teacher");
    }

    public String generateToken(String username, String teacherId, String role) {
        Date now = new Date();
        // 学生token有效期365天，老师24小时
        long expiry = "student".equals(role) ? 365L * 24 * 60 * 60 * 1000 : expirationMs;
        Date expiryDate = new Date(now.getTime() + expiry);
        var builder = Jwts.builder()
                .subject(username)
                .claim("teacherId", teacherId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate);
        return builder.signWith(key).compact();
    }

    // 学生专用token，包含studentId
    public String generateStudentToken(String studentName, String teacherId, String studentId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 365L * 24 * 60 * 60 * 1000);
        return Jwts.builder()
                .subject(studentName)
                .claim("teacherId", teacherId)
                .claim("role", "student")
                .claim("studentId", studentId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public String getTeacherIdFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().get("teacherId", String.class);
    }

    public String getRoleFromToken(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().get("role", String.class);
        } catch (Exception e) {
            return "teacher"; // 兼容旧token
        }
    }

    public String getStudentIdFromToken(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().get("studentId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}