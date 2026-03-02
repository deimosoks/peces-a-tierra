package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.auth.*;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.auth.*;
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

@Slf4j
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

        User user = (User) userDetails;

        if (!user.isActive()) {
            throw new AuthenticatedUserNotFoundException();
        }

        String accessToken = jwtService.generateToken(userDetails);

        AccessTokenDto accessTokenDto = AccessTokenDto.builder()
                .token(accessToken)
                .expiredAt(jwtService.extractExpiration(accessToken))
                .build();

        log.info("Usuario {} logueo al sistema.", user.getMember().getId());

        return AuthResponseDto.builder()
                .accessTokenDto(accessTokenDto)
                .refreshTokenDto(refreshTokenMapper.toDto(refreshTokenService.generate(user)))
                .build();
    }

    @Transactional
    @Override
    public void logout(User user, RefreshTokenRequestDto refreshTokenRequestDto) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequestDto.getRefreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!user.getUsername().equals(refreshToken.getUser().getUsername())) {
            log.warn("Usuario {} intento hacer logout el token {} que no le pertenece.", user.getMember().getId(), refreshToken.getId());
            throw new CannotLogOutWithTokenYouDoNotOwn();
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

        log.info("Usuario {} hizo refresco de sesión", newRefreshToken.getUser().getMember().getId());

        return AuthResponseDto.builder()
                .accessTokenDto(newAccessTokenDto)
                .refreshTokenDto(refreshTokenMapper.toDto(newRefreshToken))
                .build();
    }

    @Transactional
    @Override
    public void changgePassword(User user, ChanggePasswordRequest dto) {

        if (!dto.getPassword().equals(dto.getConfirmPassword()))
            throw new PasswordDoesNotMatchException();

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash()))
            throw new PasswordDoesNotMatchWithPasswordRegisterException();

        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        log.info("Usuario {} hizo cambio de contraseña.", user.getMember().getId());

        userRepository.save(user);
    }
}
