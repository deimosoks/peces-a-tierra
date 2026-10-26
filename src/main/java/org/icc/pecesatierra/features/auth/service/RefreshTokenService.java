package org.icc.pecesatierra.features.auth.service;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.auth.IssuedRefreshToken;
import org.icc.pecesatierra.features.auth.RefreshToken;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.auth.exceptions.ExpiredRefreshTokenException;
import org.icc.pecesatierra.features.auth.exceptions.InvalidRefreshTokenException;
import org.icc.pecesatierra.features.auth.repository.RefreshTokenRepository;
import org.icc.pecesatierra.utils.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * Emision, validacion y rotacion de refresh tokens (H-17).
 *
 * <p>La base de datos guarda unicamente el SHA-256 del token: el valor en claro
 * solo se entrega al cliente en el {@link IssuedRefreshToken} de respuesta. Toda
 * busqueda hashea primero el valor recibido.</p>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    /**
     * Crea una sesion nueva.
     *
     * @return el token en claro (para la respuesta) y la entidad ya hasheada
     */
    @Transactional
    public IssuedRefreshToken generate(User user) {

        String rawToken = generateSecureToken(128);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenHasher.hash(rawToken))
                .user(user)
                .createdAt(new Date(System.currentTimeMillis()))
                .expiresAt(new Date(System.currentTimeMillis() + Duration.ofDays(5).toMillis()))
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(saved.getUser(), rawToken, saved.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public void validate(String token) {

        RefreshToken refreshToken = findByRawToken(token);

        if (refreshToken.getExpiresAt().before(new Date())) {
            throw new ExpiredRefreshTokenException();
        }

    }

    /**
     * Consume el token y emite uno nuevo. Un token ya usado (o expirado) no
     * encuentra fila y produce {@link InvalidRefreshTokenException}: los refresh
     * tokens son de un solo uso.
     */
    @Transactional
    public IssuedRefreshToken validateAndRotate(String refreshTokenValue) {

        RefreshToken token = findByRawToken(refreshTokenValue);

        int deleted = refreshTokenRepository.deleteIfValid(
                tokenHasher.hash(refreshTokenValue),
                new Date()
        );
        if (deleted == 0) {
            throw new InvalidRefreshTokenException();
        }

        return generate(token.getUser());
    }

    /**
     * Busca por el hash del token recibido.
     *
     * @throws InvalidRefreshTokenException si no existe (incluye tokens emitidos
     *                                      antes de H-17, guardados en claro)
     */
    @Transactional(readOnly = true)
    public RefreshToken findByRawToken(String rawToken) {
        return refreshTokenRepository.findByToken(tokenHasher.hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    public String generateSecureToken(int byteLength) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokeBytes = new byte[byteLength];
        secureRandom.nextBytes(tokeBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokeBytes);
    }
}
