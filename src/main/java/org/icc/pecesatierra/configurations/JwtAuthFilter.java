package org.icc.pecesatierra.configurations;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.auth.exceptions.AuthenticatedUserNotFoundException;
import org.icc.pecesatierra.features.auth.service.JwtService;
import org.icc.pecesatierra.features.user.service.CustomUserDetailsServiceImpl;
import org.icc.pecesatierra.utils.models.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    static final String INVALID_TOKEN_MESSAGE = "Token inválido o expirado.";
    static final String UNAUTHORIZED_MESSAGE = "No autorizado.";

    private final JwtService jwtService;
    private final CustomUserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt;
        final String userEmail;

        try {
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                }

            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT invalido o expirado ({})", e.getClass().getSimpleName());
            writeUnauthorized(response, request, INVALID_TOKEN_MESSAGE);
            return;
        } catch (AuthenticatedUserNotFoundException | UsernameNotFoundException e) {
            log.warn("El usuario del token ya no existe.");
            writeUnauthorized(response, request, UNAUTHORIZED_MESSAGE);
            return;
        }

        filterChain.doFilter(request, response);

    }

    private void writeUnauthorized(HttpServletResponse response,
                                   HttpServletRequest request,
                                   String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponseDto.builder()
                        .localDateTime(LocalDateTime.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                        .path(request.getRequestURI())
                        .message(message)
                        .build()
        ));
    }

}
