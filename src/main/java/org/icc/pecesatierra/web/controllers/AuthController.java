package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.auth.AuthRequestDto;
import org.icc.pecesatierra.dtos.auth.AuthResponseDto;
import org.icc.pecesatierra.dtos.auth.ChanggePasswordRequest;
import org.icc.pecesatierra.dtos.auth.RefreshTokenRequestDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid AuthRequestDto authRequestDto) {
        return ResponseEntity.ok(authService.login(authRequestDto));
    }

    @PreAuthorize("@securityService.isActive(#refreshTokenRequestDto)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody @Valid RefreshTokenRequestDto refreshTokenRequestDto) {
        return ResponseEntity.ok(authService.refresh(refreshTokenRequestDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user,
                                       @RequestBody @Valid RefreshTokenRequestDto refreshTokenRequestDto) {
        authService.logout(user, refreshTokenRequestDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/changge-password")
    public ResponseEntity<Void> changgePassword(@AuthenticationPrincipal User user,
                                                @RequestBody @Valid ChanggePasswordRequest dto){
        authService.changgePassword(user, dto);
        return ResponseEntity.ok().build();
    }

}
