package org.icc.pecesatierra.unit.auth;

import org.icc.pecesatierra.features.auth.dtos.ChanggePasswordRequest;
import org.icc.pecesatierra.features.auth.exceptions.PasswordDoesNotMatchException;
import org.icc.pecesatierra.features.auth.exceptions.PasswordDoesNotMatchWithPasswordRegisterException;
import org.icc.pecesatierra.features.auth.mapper.RefreshTokenMapper;
import org.icc.pecesatierra.features.auth.repository.RefreshTokenRepository;
import org.icc.pecesatierra.features.auth.service.AuthService;
import org.icc.pecesatierra.features.auth.service.JwtService;
import org.icc.pecesatierra.features.auth.service.RefreshTokenService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.repository.UserRepository;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * M-05: el cambio de contrasena self-service debe revocar los refresh tokens del
 * usuario en la misma transaccion, igual que el reset administrativo de H-05.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user() {
        return TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));
    }

    private ChanggePasswordRequest request(String password, String confirmPassword, String oldPassword) {
        return ChanggePasswordRequest.builder()
                .password(password)
                .confirmPassword(confirmPassword)
                .oldPassword(oldPassword)
                .build();
    }

    @Test
    void passwordChangeRevokesEveryRefreshTokenOfTheUser_M05() {
        User user = user();
        when(passwordEncoder.matches("vieja-password", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("nueva-password")).thenReturn("new-hash");
        when(refreshTokenRepository.deleteByUserId(user.getId())).thenReturn(2);

        authService.changgePassword(user, request("nueva-password", "nueva-password", "vieja-password"));

        verify(refreshTokenRepository).deleteByUserId(user.getId());
        verify(userRepository).save(user);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void passwordMismatchDoesNotRevokeSessions_M05() {
        User user = user();

        assertThatThrownBy(() -> authService.changgePassword(
                user, request("nueva-password", "otra-password", "vieja-password")))
                .isInstanceOf(PasswordDoesNotMatchException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void wrongOldPasswordDoesNotRevokeSessions_M05() {
        User user = user();
        when(passwordEncoder.matches("vieja-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changgePassword(
                user, request("nueva-password", "nueva-password", "vieja-password")))
                .isInstanceOf(PasswordDoesNotMatchWithPasswordRegisterException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
