package br.com.access_control_manager.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${iam.jwt.secret}")
    private String secretKey;

    @Value("${iam.jwt.expiration-ms:900000}")
    private long jwtExpiration;

    @Value("${iam.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpiration;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails, String tenantId, boolean firstAccess) {
        return buildToken(Map.of(
                "tenant_id", tenantId,
                "first_access", firstAccess,
                "roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        ), userDetails.getUsername(), jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails, String tenantId) {
        return buildToken(Map.of("tenant_id", tenantId), userDetails.getUsername(), refreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Boolean extractFirstAccess(String token) {
        return extractAllClaims(token).get("first_access", Boolean.class);
    }

    public String extractClaim(String token, String claimName) {
        return extractAllClaims(token).get(claimName, String.class);
    }

    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        Object rolesObject = extractAllClaims(token).get("roles");

        if (!(rolesObject instanceof List<?> rolesList)) {
            return List.of();
        }

        return rolesList.stream()
                .filter(String.class::isInstance)
                .map(role -> new SimpleGrantedAuthority((String) role))
                .collect(Collectors.toList());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}