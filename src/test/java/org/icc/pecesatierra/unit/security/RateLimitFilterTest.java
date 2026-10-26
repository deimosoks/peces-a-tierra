package org.icc.pecesatierra.unit.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.icc.pecesatierra.configurations.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H-18: el filtro debe cortar la racha de intentos de login y de consultas al
 * asistente con un 429 del mismo contrato de error que el resto de la API, sin
 * tocar rutas publicas como {@code /api/health}.
 */
class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-25T12:00:00Z"));

    @Test
    void allowsTheConfiguredLoginAttemptsAndRejectsTheNextOne_H18() throws Exception {
        RateLimitFilter filter = filter(2, 5);
        AtomicInteger passed = new AtomicInteger();

        assertThat(statusOf(filter, login("ana"), passed)).isEqualTo(HttpStatus.OK.value());
        assertThat(statusOf(filter, login("ana"), passed)).isEqualTo(HttpStatus.OK.value());
        assertThat(passed).hasValue(2);

        MockHttpServletResponse rejected = perform(filter, login("ana"), passed);

        assertThat(rejected.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        JsonNode body = objectMapper.readTree(rejected.getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(429);
        assertThat(body.path("error").asText()).isEqualTo("Too Many Requests");
        assertThat(body.path("message").asText()).contains("Demasiados intentos");
        assertThat(body.path("path").asText()).isEqualTo("/api/auth/login");
        assertThat(passed).hasValue(2);
    }

    @Test
    void windowResetsSoTheUserCanRetryLater_H18() throws Exception {
        RateLimitFilter filter = filter(1, 5);
        AtomicInteger passed = new AtomicInteger();

        assertThat(statusOf(filter, login("ana"), passed)).isEqualTo(HttpStatus.OK.value());
        assertThat(statusOf(filter, login("ana"), passed)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        clock.advance(Duration.ofSeconds(60));

        assertThat(statusOf(filter, login("ana"), passed)).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void loginBodyIsStillReadableDownstream_H18() throws Exception {
        RateLimitFilter filter = filter(5, 5);
        AtomicReference<String> received = new AtomicReference<>();

        FilterChain chain = (request, response) -> {
            received.set(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            ((MockHttpServletResponse) response).setStatus(HttpStatus.OK.value());
        };

        filter.doFilter(login("ana"), new MockHttpServletResponse(), chain);

        assertThat(received.get()).contains("\"username\":\"ana\"");
    }

    @Test
    void healthEndpointIsNotRateLimited_H18() throws Exception {
        RateLimitFilter filter = filter(1, 1);
        AtomicInteger passed = new AtomicInteger();

        for (int index = 0; index < 25; index++) {
            assertThat(statusOf(filter, health(), passed)).isEqualTo(HttpStatus.OK.value());
        }

        assertThat(passed).hasValue(25);
    }

    @Test
    void assistantIsLimitedPerAuthenticatedUser_H18() throws Exception {
        RateLimitFilter filter = filter(5, 1);
        AtomicInteger passed = new AtomicInteger();

        assertThat(statusOf(filter, aiChat("ana"), passed)).isEqualTo(HttpStatus.OK.value());
        assertThat(statusOf(filter, aiChat("ana"), passed)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(statusOf(filter, aiChat("beto"), passed)).isEqualTo(HttpStatus.OK.value());
    }

    private RateLimitFilter filter(int loginMax, int aiMax) {
        return new RateLimitFilter(objectMapper, loginMax, aiMax, 60, clock);
    }

    private int statusOf(RateLimitFilter filter, MockHttpServletRequest request,
                         AtomicInteger passed) throws Exception {
        return perform(filter, request, passed).getStatus();
    }

    private MockHttpServletResponse perform(RateLimitFilter filter,
                                            MockHttpServletRequest request,
                                            AtomicInteger passed) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (chainRequest, chainResponse) -> {
            passed.incrementAndGet();
            ((MockHttpServletResponse) chainResponse).setStatus(HttpStatus.OK.value());
        };
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletRequest login(String username) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.7");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(("{\"username\":\"" + username + "\",\"password\":\"secreta\"}")
                .getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private MockHttpServletRequest aiChat(String username) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/chat");
        request.setRemoteAddr("10.0.0.7");
        request.setUserPrincipal(principal(username));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"message\":\"cuantos miembros hay\"}".getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private MockHttpServletRequest health() {
        return new MockHttpServletRequest("GET", "/api/health");
    }

    private Principal principal(String username) {
        return () -> username;
    }

    /** Reloj de prueba que solo avanza cuando el test lo pide. */
    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
