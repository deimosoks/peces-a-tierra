package org.icc.pecesatierra.features.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Emision y validacion de access tokens (JWT).
 *
 * <p>H-17: el token lleva {@code iss} (emisor), {@code aud} (audiencia) y
 * {@code jti} (identificador unico). El parser exige emisor y audiencia, de modo
 * que un token firmado con la misma clave para otro proposito no vale aqui. Los
 * tokens emitidos antes del cambio carecen de ambos claims y se rechazan: al
 * desplegar, cada sesion se renueva una vez (el access token dura 10 minutos).</p>
 */
@Service
public class JwtService {

    /** Emisor por defecto; se sobrescribe con {@code jwt.issuer}. */
    public static final String DEFAULT_ISSUER = "peces-a-tierra";

    /** Audiencia por defecto; se sobrescribe con {@code jwt.audience}. */
    public static final String DEFAULT_AUDIENCE = "peces-a-tierra-api";

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.issuer:" + DEFAULT_ISSUER + "}")
    private String issuer;

    @Value("${jwt.audience:" + DEFAULT_AUDIENCE + "}")
    private String audience;

    public SecretKey getSigningKey() {
        byte[] keyBytes = this.SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Valida firma, caducidad, emisor y audiencia.
     *
     * @throws io.jsonwebtoken.JwtException si el token no es valido o no esta
     *                                      dirigido a esta API
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()));
    }

    public String generateToken(UserDetails userDetails) {
        Date issuedAt = new Date(System.currentTimeMillis());
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + Duration.ofMinutes(10).toMillis()))
                .signWith(getSigningKey())
                .compact();
    }
}
