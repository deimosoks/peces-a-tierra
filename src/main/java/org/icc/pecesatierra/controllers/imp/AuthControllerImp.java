package org.icc.pecesatierra.controllers.imp;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.AuthController;
import org.icc.pecesatierra.dtos.auth.AuthRequestDto;
import org.icc.pecesatierra.dtos.auth.AuthResponseDto;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthControllerImp implements AuthController {

    private AuthService authService;

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid AuthRequestDto authRequestDto) {
        return ResponseEntity.ok(authService.login(authRequestDto));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody @Valid RefreshTokenRequestDto refreshTokenRequestDto) {
        return ResponseEntity.ok(authService.refresh(refreshTokenRequestDto));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user,
                                       @RequestBody @Valid RefreshTokenRequestDto refreshTokenRequestDto) {
        authService.logout(user, refreshTokenRequestDto);
        return ResponseEntity.ok().build();
    }
}
