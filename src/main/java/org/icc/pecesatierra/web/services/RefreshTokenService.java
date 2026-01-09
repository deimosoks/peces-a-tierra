package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.auth.RefreshTokenDto;
import org.icc.pecesatierra.domain.reference.RefreshToken;
import org.icc.pecesatierra.domain.reference.User;

public interface RefreshTokenService {

    RefreshTokenDto generate(User user);

    RefreshToken validate(String token);

    RefreshTokenDto rotate(RefreshToken refreshToken);

    String generateSecureToken(int byteLength);

}
