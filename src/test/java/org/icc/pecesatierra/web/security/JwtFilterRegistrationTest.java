package org.icc.pecesatierra.web.security;

import io.jsonwebtoken.MalformedJwtException;
import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.features.auth.service.JwtService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.service.CustomUserDetailsServiceImpl;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M-13: prueba la cadena de seguridad real de Spring Security con el
 * {@link JwtAuthFilter} registrado, no el filtro instanciado a mano.
 *
 * <p>Un token invalido debe producir el 401 JSON del filtro (cuerpo con mensaje),
 * no el 401 vacio del {@code HttpStatusEntryPoint}. Un token valido debe dejar
 * pasar la peticion. Quitar {@code addFilterBefore} de {@code SecurityConfig}
 * rompe ambos casos.</p>
 */
@WebMvcTest(controllers = JwtFilterRegistrationTest.ProbeController.class)
@ContextConfiguration(classes = {
        JwtFilterRegistrationTest.ProbeController.class,
        SecurityConfig.class,
        JwtAuthFilter.class,
        GlobalHandlerException.class
})
@ActiveProfiles("test")
class JwtFilterRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsServiceImpl userDetailsService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidTokenIsRejectedByTheRegisteredFilter_M13() throws Exception {
        when(jwtService.extractUsername("token-invalido"))
                .thenThrow(new MalformedJwtException("firma invalida"));

        mockMvc.perform(get("/api/probe/ping")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Token")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("pong"))));
    }

    @Test
    void validTokenAuthenticatesThroughTheRegisteredFilter_M13() throws Exception {
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));

        when(jwtService.extractUsername("token-valido")).thenReturn("operador-a");
        when(userDetailsService.loadUserByUsername("operador-a")).thenReturn(user);
        when(jwtService.isTokenValid("token-valido", user)).thenReturn(true);

        mockMvc.perform(get("/api/probe/ping")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/probe/ping")
        String ping() {
            return "pong";
        }
    }
}
