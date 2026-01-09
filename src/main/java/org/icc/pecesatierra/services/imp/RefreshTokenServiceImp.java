package org.icc.pecesatierra.services.imp;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.auth.AccessTokenDto;
import org.icc.pecesatierra.dtos.auth.AuthResponseDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.RefreshTokenException;
import org.icc.pecesatierra.mappers.RefreshTokenMapper;
import org.icc.pecesatierra.repositories.RefreshTokenRepository;
import org.icc.pecesatierra.services.JwtService;
import org.icc.pecesatierra.services.RefreshTokenService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Service
@AllArgsConstructor
public class RefreshTokenServiceImp implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public RefreshTokenDto generate(User user) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(generateSecureToken(128))
                .user(user)
                .createdAt(new Date(System.currentTimeMillis()))
                .expiresAt(new Date(System.currentTimeMillis() + 60000 * 60 * 60))
                .build();

        return refreshTokenMapper.toDto(refreshTokenRepository.save(refreshToken));
    }

    @Override
    public RefreshToken validate(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenException("Invalid RefreshToken."));

        if (refreshToken.getExpiresAt().before(new Date())){
            refreshTokenRepository.deleteByToken(token);
            throw new RefreshTokenException("Refresh token expired.");
        }

        return refreshToken;
    }

    @Transactional
    @Override
    public RefreshTokenDto rotate(RefreshToken refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken.getToken())
                .orElseThrow(() -> new RefreshTokenException("Invalid RefreshToken."));

        refreshTokenRepository.delete(token);

        return generate(token.getUser());
    }

    @Override
    public String generateSecureToken(int byteLength) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokeBytes = new byte[byteLength];
        secureRandom.nextBytes(tokeBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokeBytes);
    }
}
