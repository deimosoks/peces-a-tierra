package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.auth.AuthResponseDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface RefreshTokenService {

    RefreshTokenDto generate(User user);

    RefreshToken validate(String token);

    RefreshTokenDto rotate(RefreshToken refreshToken);

    String generateSecureToken(int byteLength);

}
