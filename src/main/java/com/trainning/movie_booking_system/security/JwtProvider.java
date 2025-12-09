package com.trainning.movie_booking_system.security;

import com.trainning.movie_booking_system.entity.Account;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtProvider {

    @Value("${jwt.accessKey}")
    private String accessKey;
    /*
     * Thời gian hết hạn của access token (tính bằng phút)
     * Thời gian hết hạn của refresh token (tính bằng ngày)
     */
    @Value("${jwt.access-token-expiry-minutes}")
    private long accessTokenExpiryMinutes;

    @Value("${jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    //================= TOKEN GENERATION =================//

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(accessKey.getBytes());
    }

    private Date getAccessTokenExpiry() {
        return new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(accessTokenExpiryMinutes));
    }

    private Date getRefreshTokenExpiry() {
        return new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(refreshTokenExpiryDays));
    }

    /**
     * Sinh access token có chứa roles và thông tin người dùng
     */
    public String generateToken(Account account) {
        String roles = account.getAccountRoles().stream()
                .map(role -> "ROLE_" + role.getRole().getName())
                .collect(Collectors.joining(","));

        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", account.getId());
        claims.put("roles", roles);

        return buildToken(claims, account.getUsername(), getAccessTokenExpiry());
    }

    /**
     * Sinh refresh token (không cần claims thêm)
     */
    public String generateRefreshToken(Account account) {
        return buildToken(new HashMap<>(), account.getUsername(), getRefreshTokenExpiry());
    }

    private String buildToken(Map<String, Object> claims, String subject, Date expiry) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    //================= TOKEN VALIDATION =================//

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    public boolean isTokenValidForAccount(String token, Account account) {
        final String username = extractUsername(token);
        return username.equals(account.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //================= TOKEN EXTRACTION =================//

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    //Get expiration
    public Date getExpiration(String token) {
         return extractClaim(token, Claims::getExpiration);
    }
}
