package org.icc.pecesatierra.configurations;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.auth.AuthenticatedUserNotFoundException;
import org.icc.pecesatierra.exceptions.auth.InvalidRefreshTokenException;
import org.icc.pecesatierra.repositories.RefreshTokenRepository;
import org.icc.pecesatierra.repositories.UserRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

@Configuration
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public boolean isActive(Authentication authentication){
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        return user.isActive();
    }

    public boolean isActive(RefreshTokenRequestDto token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token.getRefreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        return refreshToken.getUser().isActive();
    }

}
