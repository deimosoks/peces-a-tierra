package org.icc.pecesatierra.features.auth.service;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.auth.RefreshToken;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.auth.exceptions.ExpiredRefreshTokenException;
import org.icc.pecesatierra.features.auth.exceptions.InvalidRefreshTokenException;
import org.icc.pecesatierra.features.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken generate(User user) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(generateSecureToken(128))
                .user(user)
                .createdAt(new Date(System.currentTimeMillis()))
                .expiresAt(new Date(System.currentTimeMillis() + Duration.ofDays(5).toMillis()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public void validate(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.getExpiresAt().before(new Date())) {
            throw new ExpiredRefreshTokenException();
        }

    }

    @Transactional
    public RefreshToken validateAndRotate(String refreshTokenValue) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidRefreshTokenException::new);

        int deleted = refreshTokenRepository.deleteIfValid(
                refreshTokenValue,
                new Date()
        );
        if (deleted == 0) {
            throw new InvalidRefreshTokenException();
        }

        return generate(token.getUser());
    }

    public String generateSecureToken(int byteLength) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokeBytes = new byte[byteLength];
        secureRandom.nextBytes(tokeBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokeBytes);
    }
}
