package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.auth.AuthRequestDto;
import org.icc.pecesatierra.dtos.auth.AuthResponseDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.User;
import org.springframework.http.ResponseEntity;

public interface AuthController {
    ResponseEntity<AuthResponseDto> login(AuthRequestDto authRequestDto);
    ResponseEntity<AuthResponseDto> refresh(RefreshTokenRequestDto refreshTokenRequestDto);
    ResponseEntity<Void> logout(User user, RefreshTokenRequestDto refreshTokenRequestDto);
}
