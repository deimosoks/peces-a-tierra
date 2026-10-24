package org.icc.pecesatierra.unit.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.features.auth.exceptions.AuthenticatedUserNotFoundException;
import org.icc.pecesatierra.features.auth.service.JwtService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.service.CustomUserDetailsServiceImpl;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Contrato del filtro JWT (H-12): cualquier token invalido, expirado, mal
 * firmado o asociado a un usuario borrado responde 401 con un mensaje fijo y no
 * deja que la excepcion llegue al manejador global (que devolveria 500).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String INVALID_TOKEN_MESSAGE = "Token inválido o expirado.";
    private static final String UNAUTHORIZED_MESSAGE = "No autorizado.";

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private JwtAuthFilter filter;
    private User domainUser;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService, objectMapper);
        domainUser = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"),
                "VIEW_MEMBER_PANEL");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidSignatureReturns401WithFixedMessage_H12() throws Exception {
        when(jwtService.extractUsername("firmado-con-otra-clave"))
                .thenThrow(new SignatureException("JWT signature does not match locally computed signature"));

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = performRequest("firmado-con-otra-clave", chain);

        assertUnauthorized(response, chain, INVALID_TOKEN_MESSAGE);
        assertThat(response.getContentAsString()).doesNotContain("signature");
    }

    @Test
    void malformedTokenReturns401WithFixedMessage_H12() throws Exception {
        when(jwtService.extractUsername("no-es-un-jwt"))
                .thenThrow(new MalformedJwtException("Unable to read JSON value"));

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = performRequest("no-es-un-jwt", chain);

        assertUnauthorized(response, chain, INVALID_TOKEN_MESSAGE);
    }

    @Test
    void expiredTokenReturns401WithFixedMessage_H12() throws Exception {
        when(jwtService.extractUsername("expirado"))
                .thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = performRequest("expirado", chain);

        assertUnauthorized(response, chain, INVALID_TOKEN_MESSAGE);
    }

    @Test
    void deletedUserReturns401WithoutLeakingDetails_H12() throws Exception {
        when(jwtService.extractUsername("token-de-usuario-borrado")).thenReturn("usuario-borrado");
        when(userDetailsService.loadUserByUsername("usuario-borrado"))
                .thenThrow(new AuthenticatedUserNotFoundException());

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = performRequest("token-de-usuario-borrado", chain);

        assertUnauthorized(response, chain, UNAUTHORIZED_MESSAGE);
    }

    @Test
    void signatureFailureWhileValidatingTokenReturns401_H12() throws Exception {
        when(jwtService.extractUsername("token-manipulado")).thenReturn("operador-a");
        when(userDetailsService.loadUserByUsername("operador-a")).thenReturn(domainUser);
        when(jwtService.isTokenValid("token-manipulado", domainUser))
                .thenThrow(new SignatureException("JWT signature does not match"));

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = performRequest("token-manipulado", chain);

        assertUnauthorized(response, chain, INVALID_TOKEN_MESSAGE);
    }

    @Test
    void validTokenSetsAuthenticationAndContinuesTheChain() throws Exception {
        when(jwtService.extractUsername("token-valido")).thenReturn("operador-a");
        when(userDetailsService.loadUserByUsername("operador-a")).thenReturn(domainUser);
        when(jwtService.isTokenValid("token-valido", domainUser)).thenReturn(true);

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = performRequest("token-valido", chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(domainUser);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("VIEW_MEMBER_PANEL");
    }

    @Test
    void requestWithoutAuthorizationHeaderContinuesWithoutAuthenticating() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    private MockHttpServletResponse performRequest(String token, MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        return response;
    }

    private void assertUnauthorized(MockHttpServletResponse response,
                                    MockFilterChain chain,
                                    String expectedMessage) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(chain.getRequest()).as("la cadena no debe continuar con un token invalido").isNull();

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asText()).isEqualTo(expectedMessage);
        assertThat(body.get("path").asText()).isEqualTo("/api/members");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
