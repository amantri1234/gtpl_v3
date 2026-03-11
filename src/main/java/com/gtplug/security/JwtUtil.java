package com.gtplug.security;

import com.gtplug.config.DatabaseConfig;
import com.gtplug.models.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String SECRET = DatabaseConfig.getJwtSecret();
    private static final long EXPIRATION_MS = DatabaseConfig.getJwtExpiration();
    private static final String ISSUER = "gtplug-ug-work-system";
    private static final String AUDIENCE = "gtplug-users";
    
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(EXPIRATION_MS, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    public static Claims validateToken(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(KEY)
                    .requireIssuer(ISSUER)
                    .build();
            
            return parser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            throw new SecurityException("Token has expired");
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            throw new SecurityException("Unsupported token");
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            throw new SecurityException("Malformed token");
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw new SecurityException("Invalid token signature");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is empty or null: {}", e.getMessage());
            throw new SecurityException("Token is empty");
        }
    }

    public static boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("username", String.class);
    }

    public static User.UserRole getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        String roleStr = claims.get("role", String.class);
        return User.UserRole.valueOf(roleStr);
    }

    public static Date getExpirationDateFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration();
    }

    public static boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public static long getExpirationTime() {
        return EXPIRATION_MS;
    }
}
