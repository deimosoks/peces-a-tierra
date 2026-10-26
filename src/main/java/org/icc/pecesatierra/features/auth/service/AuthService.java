package org.icc.pecesatierra.features.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.auth.dtos.*;
import org.icc.pecesatierra.features.auth.RefreshToken;
import org.icc.pecesatierra.features.auth.exceptions.*;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.auth.repository.RefreshTokenRepository;
import org.icc.pecesatierra.features.user.repository.UserRepository;
import org.icc.pecesatierra.features.auth.mapper.RefreshTokenMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Hash BCrypt senuelo (coste 10, el mismo que el {@code PasswordEncoder} de
     * produccion) contra el que se compara la contrasena cuando el usuario no
     * existe o esta inactivo (H-19).
     *
     * <p>Sin esta comparacion el camino de "usuario inexistente" terminaba mucho
     * antes que el de "usuario existente" y permitia enumerar cuentas midiendo el
     * tiempo de respuesta. El valor es una constante publica a proposito: su
     * resultado se descarta y solo aporta el coste de computo.</p>
     */
    private static final String DECOY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Transactional
    public AuthResponseDto login(AuthRequestDto authRequestDto) {

        UserDetails userDetails;

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequestDto.getUsername(),
                            authRequestDto.getPassword()
                    )
            );

            userDetails = userDetailsService.loadUserByUsername(authRequestDto.getUsername());
        } catch (AuthenticationException authenticationException) {
            // H-19: mismo coste de hash que el camino valido antes de propagar el
            // error generico, para no distinguir "no existe" de "contrasena mala".
            passwordEncoder.matches(authRequestDto.getPassword(), DECOY_PASSWORD_HASH);
            throw authenticationException;
        }

        User user = (User) userDetails;

        if (!user.isActive()) {
            // H-19: el usuario inactivo tampoco puede ser distinguible por tiempo.
            passwordEncoder.matches(authRequestDto.getPassword(), DECOY_PASSWORD_HASH);
            throw new AuthenticatedUserNotFoundException();
        }

        String accessToken = jwtService.generateToken(userDetails);

        AccessTokenDto accessTokenDto = AccessTokenDto.builder()
                .token(accessToken)
                .expiredAt(jwtService.extractExpiration(accessToken))
                .build();

        log.info("""
                        Usuario: 
                        Id: {}
                        Name: {}
                        Logueo al sistema.
                        """
                , user.getMember().getId()
                , user.getMember().getCompleteName());

        return AuthResponseDto.builder()
                .accessTokenDto(accessTokenDto)
                .refreshTokenDto(refreshTokenMapper.toDto(refreshTokenService.generate(user)))
                .build();
    }

    @Transactional
    public void logout(User user, RefreshTokenRequestDto refreshTokenRequestDto) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequestDto.getRefreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!user.getUsername().equals(refreshToken.getUser().getUsername())) {
            log.warn("""
                            Usuario:
                             Id: {} 
                             Name: {}
                             Intento hacer logout el token {} que no le pertenece.
                            """
                    , user.getMember().getId(), refreshToken.getId()
                    , user.getMember().getCompleteName());
            throw new CannotLogOutWithTokenYouDoNotOwn();
        }

        refreshTokenRepository.deleteByToken(refreshToken.getToken());

    }

    @Transactional
    public AuthResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto) {

        RefreshToken newRefreshToken = refreshTokenService.validateAndRotate(refreshTokenRequestDto.getRefreshToken());

        String accessToken = jwtService.generateToken(newRefreshToken.getUser());

        AccessTokenDto newAccessTokenDto = AccessTokenDto.builder()
                .token(accessToken)
                .expiredAt(jwtService.extractExpiration(accessToken))
                .build();

        log.info("""
                        Usuario: 
                        Id: {}
                        Name: {}
                        Hizo refresco de sesión.
                        """
                , newRefreshToken.getUser().getMember().getId()
                , newRefreshToken.getUser().getMember().getCompleteName());
        return AuthResponseDto.builder()
                .accessTokenDto(newAccessTokenDto)
                .refreshTokenDto(refreshTokenMapper.toDto(newRefreshToken))
                .build();
    }

    @Transactional
    public void changgePassword(User user, ChanggePasswordRequest dto) {

        if (!dto.getPassword().equals(dto.getConfirmPassword()))
            throw new PasswordDoesNotMatchException();

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash()))
            throw new PasswordDoesNotMatchWithPasswordRegisterException();

        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        // Revoca los refresh tokens del usuario en la misma transaccion: si la
        // contrasena cambia, una sesion robada no debe poder refrescar. Se hace por
        // repositorio porque el principal esta desacoplado y la coleccion es LAZY.
        int revokedSessions = refreshTokenRepository.deleteByUserId(user.getId());

        log.info("""
                        Usuario: 
                        Id: {}
                        Name: {}
                        Cambio su contraseña. Sesiones revocadas: {}
                        """
                , user.getMember().getId()
                , user.getMember().getCompleteName()
                , revokedSessions);

        userRepository.save(user);
    }
}
