package FinanceManangementSystem.demo.Security;

import FinanceManangementSystem.demo.Model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secretKey}")
    private String secret;

    private Key getSignKey() {
        log.debug("JwtUtil - Generating JWT signing key.");
        // Must explicitly use UTF_8 to get identical byte arrays on Windows (local)
        // and Linux (Render Docker container). Without this, signing may use different
        // platform charsets and token verification will fail intermittently.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {

        log.info("JwtUtil - Generating JWT access token for user: {}", user.getEmail());

        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("contact", user.getMobileNumber())
                .claim("publicId", user.getPublicId().toString())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();

        log.info("JwtUtil - JWT access token generated successfully for user: {}", user.getEmail());

        return token;
    }

    public String extractEmail(String token) {

        log.debug("JwtUtil - Extracting email from JWT token.");

        String email = getClaims(token).getSubject();

        log.debug("JwtUtil - Email extracted successfully: {}", email);

        return email;

    }

    public boolean validateToken(String token, UserDetails userDetails) {

        log.debug("JwtUtil - Validating JWT token for user: {}", userDetails.getUsername());

        String email = extractEmail(token);

        boolean isValid = email.equals(userDetails.getUsername()) && !isTokenExpired(token);

        if (isValid) {
            log.info("JwtUtil - JWT token validation successful for user: {}", email);
        }
        else {
            log.warn("JwtUtil - JWT token validation failed for user: {}", email);
        }

        return isValid;

    }

    private boolean isTokenExpired(String token) {

        boolean expired = getClaims(token)
                .getExpiration()
                .before(new Date());

        if (expired) {
            log.warn("JwtUtil - JWT token has expired.");
        }

        return expired;
    }

    private Claims getClaims(String token) {

        log.debug("JwtUtil - Parsing JWT claims.");

        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}