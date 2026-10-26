package org.icc.pecesatierra.web.security;

import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.RateLimitFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.features.auth.service.JwtService;
import org.icc.pecesatierra.features.user.service.CustomUserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H-18: el filtro debe registrarse en la cadena real de Spring Security (no solo
 * instanciado a mano), respetar los limites configurados por propiedad y no
 * afectar a las rutas publicas.
 */
@WebMvcTest(controllers = RateLimitFilterRegistrationTest.ProbeController.class)
@ContextConfiguration(classes = {
        RateLimitFilterRegistrationTest.ProbeController.class,
        SecurityConfig.class,
        JwtAuthFilter.class,
        RateLimitFilter.class,
        GlobalHandlerException.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.rate-limit.login-max=2",
        "security.rate-limit.ai-max=2",
        "security.rate-limit.window-seconds=60"
})
class RateLimitFilterRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsServiceImpl userDetailsService;

    @Test
    void loginIsRejectedWithTheContractErrorAfterTheConfiguredAttempts_H18() throws Exception {
        String body = "{\"username\":\"operador-a\",\"password\":\"secreta\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Demasiados intentos")))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void healthKeepsAnsweringAfterManyRequests_H18() throws Exception {
        for (int index = 0; index < 10; index++) {
            mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        }
    }

    @RestController
    static class ProbeController {

        @PostMapping(path = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
        String login(@RequestBody Map<String, String> body) {
            return body.get("username");
        }

        @GetMapping("/api/health")
        String health() {
            return "ok";
        }
    }
}
