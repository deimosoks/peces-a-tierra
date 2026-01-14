package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.auth.AuthRequestDto;
import org.icc.pecesatierra.dtos.auth.AuthResponseDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.domain.entities.User;

public interface AuthService {

    AuthResponseDto login(AuthRequestDto authRequestDto);

    void logout(User user, RefreshTokenRequestDto refreshTokenRequestDto);

    AuthResponseDto refresh(RefreshTokenRequestDto refreshTokenRequestDto);

}
