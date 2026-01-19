package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.entities.RefreshToken;
import org.icc.pecesatierra.entities.User;

public interface RefreshTokenService {

    RefreshToken generate(User user);

    void validate(String token);

    RefreshToken validateAndRotate(String token);

    String generateSecureToken(int byteLength);

}
