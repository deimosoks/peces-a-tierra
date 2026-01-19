package org.icc.pecesatierra.configurations;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.RefreshTokenException;
import org.icc.pecesatierra.repositories.RefreshTokenRepository;
import org.icc.pecesatierra.repositories.UserRepository;
import org.icc.pecesatierra.web.services.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@AllArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;

    public boolean isActive(Authentication authentication){
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Error de validación, por favor intente mas tarde."));

        return user.isActive();
    }

    public boolean isActive(RefreshTokenRequestDto token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token.getRefreshToken())
                .orElseThrow(()-> new RefreshTokenException("Session token invalido."));

        return refreshToken.getUser().isActive();
    }

}
