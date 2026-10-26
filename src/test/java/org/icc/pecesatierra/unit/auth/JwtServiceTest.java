package org.icc.pecesatierra.unit.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.icc.pecesatierra.features.auth.service.JwtService;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * H-17: el access token debe identificar emisor, audiencia e identificador unico,
 * y el parser debe rechazar tokens de otro emisor u otra audiencia aunque la
 * firma sea correcta.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", SECRET);
        ReflectionTestUtils.setField(jwtService, "issuer", JwtService.DEFAULT_ISSUER);
        ReflectionTestUtils.setField(jwtService, "audience", JwtService.DEFAULT_AUDIENCE);
    }

    @Test
    void generatedTokenCarriesIssuerAudienceAndJti_H17() {
        String token = jwtService.generateToken(TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A")));

        Claims claims = jwtService.extractAllClaims(token);

        assertThat(claims.getIssuer()).isEqualTo(JwtService.DEFAULT_ISSUER);
        assertThat(claims.getAudience()).contains(JwtService.DEFAULT_AUDIENCE);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("operador-a");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void eachTokenHasItsOwnJti_H17() {
        var user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));

        String first = jwtService.extractAllClaims(jwtService.generateToken(user)).getId();
        String second = jwtService.extractAllClaims(jwtService.generateToken(user)).getId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected_H17() {
        String foreignToken = manualToken(JwtService.DEFAULT_ISSUER + "-otro",
                JwtService.DEFAULT_AUDIENCE);

        assertThatThrownBy(() -> jwtService.extractAllClaims(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenForAnotherAudienceIsRejected_H17() {
        String foreignToken = manualToken(JwtService.DEFAULT_ISSUER,
                JwtService.DEFAULT_AUDIENCE + "-otro");

        assertThatThrownBy(() -> jwtService.extractAllClaims(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void legacyTokenWithoutIssuerOrAudienceIsRejected_H17() {
        String legacyToken = Jwts.builder()
                .subject("operador-a")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()))
                .signWith(key())
                .compact();

        assertThatThrownBy(() -> jwtService.extractAllClaims(legacyToken))
                .isInstanceOf(JwtException.class);
    }

    private String manualToken(String issuer, String audience) {
        return Jwts.builder()
                .subject("operador-a")
                .issuer(issuer)
                .audience().add(audience).and()
                .id("jti-de-prueba")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()))
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
