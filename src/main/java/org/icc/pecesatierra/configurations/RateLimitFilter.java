package org.icc.pecesatierra.configurations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.utils.models.ErrorResponseDto;
import org.icc.pecesatierra.utils.security.InMemoryRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Limita los intentos de login y las consultas al asistente de IA (H-18).
 *
 * <p>Antes de este filtro {@code POST /api/auth/login} aceptaba intentos
 * ilimitados (fuerza bruta de contrasenas) y {@code POST /api/ai/chat} tambien
 * (coste por token y abuso del proveedor). El limite es una ventana fija en
 * memoria, por clave:</p>
 * <ul>
 *   <li>login: IP remota + usuario del cuerpo, {@code security.rate-limit.login-max}
 *       intentos por ventana (10 por defecto);</li>
 *   <li>asistente: usuario autenticado (o IP si no hay principal),
 *       {@code security.rate-limit.ai-max} (20 por defecto).</li>
 * </ul>
 *
 * <p>La ventana la fija {@code security.rate-limit.window-seconds} (60 por
 * defecto). Al exceder se responde <b>429</b> con el mismo {@link ErrorResponseDto}
 * del resto de errores y cabecera {@code Retry-After}.</p>
 *
 * <p>Se usa {@code getRemoteAddr()} y no {@code X-Forwarded-For} a proposito:
 * esa cabecera la controla el cliente y permitiria saltarse el limite rotando
 * valores falsos.</p>
 *
 * <p>Para poder discriminar por usuario en el login sin romper el cuerpo de la
 * peticion, el filtro cachea y reproduce el JSON (solo si el
 * {@code Content-Length} es razonable).</p>
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = "/api/auth/login";
    static final String AI_PATH = "/api/ai/chat";

    static final String LOGIN_LIMIT_MESSAGE =
            "Demasiados intentos de inicio de sesión. Espere un momento antes de volver a intentarlo.";
    static final String AI_LIMIT_MESSAGE =
            "Demasiadas consultas al asistente. Espere un momento antes de volver a intentarlo.";

    private static final String UNKNOWN = "desconocido";
    private static final int MAX_CACHED_BODY_BYTES = 8 * 1024;
    private static final int MAX_KEY_PART_LENGTH = 64;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ObjectMapper objectMapper;
    private final InMemoryRateLimiter loginLimiter;
    private final InMemoryRateLimiter aiLimiter;
    private final long windowSeconds;

    public RateLimitFilter(ObjectMapper objectMapper,
                           @Value("${security.rate-limit.login-max:10}") int loginMax,
                           @Value("${security.rate-limit.ai-max:20}") int aiMax,
                           @Value("${security.rate-limit.window-seconds:60}") long windowSeconds,
                           Clock clock) {
        this.objectMapper = objectMapper;
        this.windowSeconds = Math.max(1, windowSeconds);
        Duration window = Duration.ofSeconds(this.windowSeconds);
        this.loginLimiter = new InMemoryRateLimiter(Math.max(1, loginMax), window, MAX_TRACKED_KEYS, clock);
        this.aiLimiter = new InMemoryRateLimiter(Math.max(1, aiMax), window, MAX_TRACKED_KEYS, clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !LOGIN_PATH.equals(path) && !AI_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (LOGIN_PATH.equals(path)) {
            CachedBodyHttpServletRequest cachedRequest = cacheBody(request);
            HttpServletRequest loginRequest = cachedRequest != null ? cachedRequest : request;
            String username = cachedRequest != null ? extractUsername(cachedRequest) : UNKNOWN;
            String key = loginKey(clientIp(loginRequest), username);

            if (!loginLimiter.tryAcquire(key)) {
                log.warn("Limite de intentos de login excedido.");
                writeTooManyRequests(response, path, LOGIN_LIMIT_MESSAGE);
                return;
            }
            filterChain.doFilter(loginRequest, response);
            return;
        }

        String key = aiKey(principal(request), clientIp(request));
        if (!aiLimiter.tryAcquire(key)) {
            log.warn("Limite de consultas al asistente excedido.");
            writeTooManyRequests(response, path, AI_LIMIT_MESSAGE);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String loginKey(String ip, String username) {
        return "login|" + ip + "|" + username;
    }

    private String aiKey(String principal, String ip) {
        return "ai|" + (principal == null || principal.isBlank() ? "ip:" + ip : principal);
    }

    private String principal(HttpServletRequest request) {
        if (request.getUserPrincipal() == null) {
            return null;
        }
        return truncate(request.getUserPrincipal().getName().toLowerCase(Locale.ROOT));
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? UNKNOWN : remoteAddress;
    }

    private String extractUsername(HttpServletRequest request) {
        try {
            JsonNode username = objectMapper.readTree(request.getInputStream()).path("username");
            if (username.isTextual() && !username.asText().isBlank()) {
                return truncate(username.asText().toLowerCase(Locale.ROOT));
            }
        } catch (IOException | RuntimeException e) {
            // Cuerpo ilegible o vacio: la peticion seguira su curso y el
            // controller respondera 400. Solo perdemos la parte "usuario" de la
            // clave, no el limite por IP.
            log.debug("No se pudo leer el usuario del intento de login.");
        }
        return UNKNOWN;
    }

    private String truncate(String value) {
        return value.length() <= MAX_KEY_PART_LENGTH ? value : value.substring(0, MAX_KEY_PART_LENGTH);
    }

    /**
     * Devuelve una peticion cuyo cuerpo puede leerse dos veces (filtro y
     * controller). Si el cuerpo es demasiado grande o no anuncia longitud, se
     * devuelve {@code null}: la peticion original no se toca y la clave de login
     * se queda sin usuario.
     */
    private CachedBodyHttpServletRequest cacheBody(HttpServletRequest request) throws IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength < 0 || contentLength > MAX_CACHED_BODY_BYTES) {
            return null;
        }

        byte[] body = request.getInputStream().readAllBytes();
        return new CachedBodyHttpServletRequest(request, body);
    }

    private void writeTooManyRequests(HttpServletResponse response,
                                      String path,
                                      String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponseDto.builder()
                        .localDateTime(LocalDateTime.now())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                        .message(message)
                        .path(path)
                        .build()
        ));
    }

    /** Peticion con el cuerpo ya leido que se puede volver a leer. */
    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return stream.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return stream.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Lectura no asincrona");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                    new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
