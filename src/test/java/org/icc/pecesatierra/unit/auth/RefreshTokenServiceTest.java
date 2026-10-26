package org.icc.pecesatierra.unit.auth;

import org.icc.pecesatierra.features.auth.IssuedRefreshToken;
import org.icc.pecesatierra.features.auth.RefreshToken;
import org.icc.pecesatierra.features.auth.exceptions.InvalidRefreshTokenException;
import org.icc.pecesatierra.features.auth.repository.RefreshTokenRepository;
import org.icc.pecesatierra.features.auth.service.RefreshTokenService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-17: la base de datos no puede contener el refresh token en claro y un token
 * ya consumido (o desconocido) debe fallar aunque su valor sea sintacticamente
 * valido.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String PEPPER = "pepper-de-prueba";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private TokenHasher tokenHasher;
    private RefreshTokenService refreshTokenService;

    private final User user = TestFixtures.user("user-1", "operador-a",
            TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));

    @BeforeEach
    void setUp() {
        tokenHasher = new TokenHasher(PEPPER);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, tokenHasher);
    }

    @Test
    void plainTokenIsNeverPersisted_H17() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken issued = refreshTokenService.generate(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());

        assertThat(issued.token()).isNotBlank();
        assertThat(saved.getValue().getToken())
                .isEqualTo(tokenHasher.hash(issued.token()))
                .isNotEqualTo(issued.token())
                .doesNotContain(issued.token())
                .hasSize(64);
    }

    @Test
    void hashIsDeterministicAndDependsOnThePepper_H17() {
        String hash = tokenHasher.hash("token-en-claro");

        assertThat(tokenHasher.hash("token-en-claro")).isEqualTo(hash);
        assertThat(new TokenHasher("otro-pepper").hash("token-en-claro")).isNotEqualTo(hash);
        assertThat(tokenHasher.hash(null)).isEmpty();
    }

    @Test
    void validRefreshTokenRotatesIntoANewOne_H17() {
        String rawToken = "token-vigente";
        RefreshToken stored = storedToken(tokenHasher.hash(rawToken));

        when(refreshTokenRepository.findByToken(tokenHasher.hash(rawToken))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.deleteIfValid(eq(tokenHasher.hash(rawToken)), any(Date.class)))
                .thenReturn(1);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken rotated = refreshTokenService.validateAndRotate(rawToken);

        assertThat(rotated.token()).isNotBlank().isNotEqualTo(rawToken);
        assertThat(rotated.user()).isSameAs(user);
        verify(refreshTokenRepository).findByToken(tokenHasher.hash(rawToken));
        verify(refreshTokenRepository, never()).findByToken(rawToken);
    }

    @Test
    void alreadyUsedRefreshTokenIsRejected_H17() {
        String rawToken = "token-ya-usado";
        RefreshToken stored = storedToken(tokenHasher.hash(rawToken));

        when(refreshTokenRepository.findByToken(tokenHasher.hash(rawToken))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.deleteIfValid(eq(tokenHasher.hash(rawToken)), any(Date.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void plaintextTokenFromBeforeTheFixIsRejected_H17() {
        // Una fila antigua guardaba el valor en claro: al hashear la entrada ya no
        // coincide y la sesion se considera invalida.
        when(refreshTokenRepository.findByToken(tokenHasher.hash("token-antiguo")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.findByRawToken("token-antiguo"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private RefreshToken storedToken(String hash) {
        return RefreshToken.builder()
                .id("refresh-1")
                .token(hash)
                .user(user)
                .createdAt(new Date())
                .expiresAt(new Date(System.currentTimeMillis() + 60_000))
                .build();
    }
}
