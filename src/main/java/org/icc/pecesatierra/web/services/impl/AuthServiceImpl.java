package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.dtos.auth.*;
import org.icc.pecesatierra.exceptions.RefreshTokenException;
import org.icc.pecesatierra.exceptions.UserDeactivatedException;
import org.icc.pecesatierra.exceptions.UserNotFoundException;
import org.icc.pecesatierra.repositories.RefreshTokenRepository;
import org.icc.pecesatierra.repositories.UserRepository;
import org.icc.pecesatierra.utils.mappers.RefreshTokenMapper;
import org.icc.pecesatierra.web.services.AuthService;
import org.icc.pecesatierra.web.services.JwtService;
import org.icc.pecesatierra.web.services.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private RefreshTokenService refreshTokenService;
    private JwtService jwtService;
    private RefreshTokenRepository refreshTokenRepository;
    private AuthenticationManager authenticationManager;
    private UserDetailsService userDetailsService;
    private UserRepository userRepository;
    private RefreshTokenMapper refreshTokenMapper;

    @Transactional
    @Override
    public AuthResponseDto login(AuthRequestDto authRequestDto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequestDto.getUsername(),
                        authRequestDto.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequestDto.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Este usuario no existe."));

        if (!user.isActive()){
            throw new UserDeactivatedException("El usuario con el que si intenta logear esta desactivado.");
        }

        String accessToken = jwtService.generateToken(userDetails);

        AccessTokenDto accessTokenDto = AccessTokenDto.builder()
                .token(accessToken)
                .expiredAt(jwtService.extractExpiration(accessToken))
                .build();

        return AuthResponseDto.builder()
                .accessTokenDto(accessTokenDto)
                .refreshTokenDto(refreshTokenMapper.toDto(refreshTokenService.generate(user)))
                .build();
    }

    @Transactional
    @Override
    public void logout(User user, RefreshTokenRequestDto refreshTokenRequestDto) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequestDto.getRefreshToken())
                .orElseThrow(() -> new RefreshTokenException("Session token invalido."));

        if (!user.getUsername().equals(refreshToken.getUser().getUsername())) {
            throw new RefreshTokenException("No eres el dueño de este session token.");
        }

        refreshTokenRepository.deleteByToken(refreshToken.getToken());

    }

    @Transactional
    @Override
    public AuthResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto) {

//        refreshTokenService.validate(refreshTokenRequestDto.getRefreshToken());

        RefreshToken newRefreshToken = refreshTokenService.validateAndRotate(refreshTokenRequestDto.getRefreshToken());

         String accessToken = jwtService.generateToken(newRefreshToken.getUser());

        AccessTokenDto newAccessTokenDto = AccessTokenDto.builder()
                .token(accessToken)
                .expiredAt(jwtService.extractExpiration(accessToken))
                .build();

        return AuthResponseDto.builder()
                .accessTokenDto(newAccessTokenDto)
                .refreshTokenDto(refreshTokenMapper.toDto(newRefreshToken))
                .build();
    }
}
