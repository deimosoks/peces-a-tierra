package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.dtos.auth.*;
import org.icc.pecesatierra.exceptions.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

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

        if (!user.isActive()) {
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

    @Transactional
    @Override
    public void changgePassword(User user, ChanggePasswordRequest dto) {

        if (!dto.getPassword().equals(dto.getConfirmPassword()))
            throw new PasswordDoesNotMatchException("Las contraseñas no coinciden.");

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash()))
            throw new PasswordDoesNotMatchWithPasswordRegisterException("La contraseña anterior no coincide con su contraseña registrada.");

        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);
    }
}
