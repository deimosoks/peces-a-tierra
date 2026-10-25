package org.icc.pecesatierra.web.user;

import jakarta.servlet.FilterChain;
import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.configurations.SecurityService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.controller.UserController;
import org.icc.pecesatierra.features.user.dtos.UserResponseDto;
import org.icc.pecesatierra.features.user.service.UserService;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de regresion de H-42b: dar de alta un usuario sin contraseña llegaba a
 * {@code passwordEncoder.encode(null)} y respondia 500. La obligatoriedad se
 * valida solo en el alta, de modo que {@code PUT /api/users/{id}} sin contraseña
 * (que hoy es un caso legitimo) sigue funcionando.
 */
@WebMvcTest
@ContextConfiguration(classes = {
        UserController.class,
        SecurityConfig.class,
        GlobalHandlerException.class
})
@ActiveProfiles("test")
class UserControllerValidationTest {

    private static final String CREATE_WITHOUT_PASSWORD = """
            {
              "memberId": "member-1",
              "username": "usuario-nuevo",
              "rolesId": ["role-1"]
            }
            """;

    private static final String UPDATE_WITHOUT_PASSWORD = """
            {
              "memberId": "member-1",
              "username": "usuario-editado",
              "rolesId": ["role-1"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean(name = "securityService")
    private SecurityService securityService;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private User domainUser;

    @BeforeEach
    void setUp() throws Exception {
        domainUser = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("member-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));
        when(securityService.isActive(any(Authentication.class))).thenReturn(true);

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void createUserWithoutPasswordIs400_H42B() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_WITHOUT_PASSWORD)
                        .with(authentication(authFor("CREATE_USER"))))
                .andExpect(status().isBadRequest());

        // Antes del fix el DTO pasaba la validacion y el NPE ocurria en el servicio.
        verify(userService, never()).create(any(), any(User.class));
    }

    @Test
    void createUserWithoutUsernameIsStill400_H42B() throws Exception {
        // El @Validated del alta incluye el grupo Default: el resto de
        // restricciones del DTO siguen evaluandose.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "member-1",
                                  "password": "password123",
                                  "rolesId": ["role-1"]
                                }
                                """)
                        .with(authentication(authFor("CREATE_USER"))))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(any(), any(User.class));
    }

    @Test
    void updateUserWithoutPasswordStillSucceeds_H42B() throws Exception {
        when(userService.update(any(), eq("user-2"), any(User.class)))
                .thenReturn(UserResponseDto.builder().id("user-2").username("usuario-editado").build());

        mockMvc.perform(put("/api/users/user-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_WITHOUT_PASSWORD)
                        .with(authentication(authFor("UPDATE_USER"))))
                .andExpect(status().isOk());

        verify(userService).update(any(), eq("user-2"), eq(domainUser));
    }

    private Authentication authFor(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                domainUser,
                "not-used",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}
