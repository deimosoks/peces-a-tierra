package org.icc.pecesatierra.web.security;

import jakarta.servlet.FilterChain;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.features.health.HealthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H-16: CORS ya no acepta cualquier origen con credenciales. Solo los origenes
 * explicitos de {@code cors.allowed-origins} reciben cabeceras; un origen
 * desconocido no recibe {@code Access-Control-Allow-Origin}. Si la propiedad
 * queda vacia no se registra ninguna configuracion CORS.
 */
@WebMvcTest(controllers = HealthController.class)
@ContextConfiguration(classes = {
        HealthController.class,
        SecurityConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "cors.allowed-origins=http://allowed.example")
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void allowedOriginReceivesTheCorsHeader_H16() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(HttpHeaders.ORIGIN, "http://allowed.example"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://allowed.example"));
    }

    @Test
    void unknownOriginDoesNotReceiveTheCorsHeader_H16() throws Exception {
        // Spring rechaza la peticion CORS con 403 y, sobre todo, sin cabecera
        // Access-Control-Allow-Origin: el navegador no puede leer la respuesta.
        mockMvc.perform(get("/api/health")
                        .header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void emptyConfigurationRegistersNoCors_H16() {
        SecurityConfig config = new SecurityConfig(authenticationProvider, jwtAuthFilter);
        ReflectionTestUtils.setField(config, "allowedOrigins", " ");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(HttpHeaders.ORIGIN, "http://allowed.example");

        assertThat(config.corsConfigurationSource().getCorsConfiguration(request)).isNull();
    }

    @Test
    void commaSeparatedOriginsAreAccepted_H16() {
        SecurityConfig config = new SecurityConfig(authenticationProvider, jwtAuthFilter);
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://one.example, http://two.example");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(HttpHeaders.ORIGIN, "http://two.example");

        assertThat(config.corsConfigurationSource().getCorsConfiguration(request))
                .isNotNull()
                .satisfies(cors -> assertThat(cors.checkOrigin("http://two.example"))
                        .isEqualTo("http://two.example"));
        assertThat(config.corsConfigurationSource().getCorsConfiguration(request).checkOrigin("http://evil.example"))
                .isNull();
    }
}
