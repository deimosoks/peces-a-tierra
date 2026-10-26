package org.icc.pecesatierra.configurations;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.auth.dtos.RefreshTokenRequestDto;
import org.icc.pecesatierra.features.auth.RefreshToken;
import org.icc.pecesatierra.features.auth.service.RefreshTokenService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.auth.exceptions.AuthenticatedUserNotFoundException;
import org.icc.pecesatierra.features.user.repository.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public boolean isActive(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        return user.isActive();
    }

    @Transactional(readOnly = true)
    public boolean isActive(RefreshTokenRequestDto token) {
        // H-17: el refresh token se guarda hasheado, la busqueda tambien.
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token.getRefreshToken());

        return refreshToken.getUser().isActive();
    }

}
