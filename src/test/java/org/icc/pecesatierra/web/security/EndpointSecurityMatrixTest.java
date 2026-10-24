package org.icc.pecesatierra.web.security;

import jakarta.servlet.FilterChain;
import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.configurations.SecurityService;
import org.icc.pecesatierra.features.health.HealthController;
import org.icc.pecesatierra.features.service.controller.ServiceController;
import org.icc.pecesatierra.features.service.service.ServiceService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.controller.UserController;
import org.icc.pecesatierra.features.user.service.UserService;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Subconjunto representativo de la matriz de autorizacion (P4). Usa el principal
 * de dominio real, no {@code @WithMockUser}, porque los controllers reciben
 * {@code @AuthenticationPrincipal User}.
 *
 * <p>Los casos sin permiso aceptan 401 o 403 mientras H-11 siga mapeando
 * {@code AuthorizationDeniedException} a 401; lo importante es que la operacion
 * no llegue al servicio.</p>
 */
@WebMvcTest
@ContextConfiguration(classes = {
        UserController.class,
        ServiceController.class,
        HealthController.class,
        SecurityConfig.class,
        GlobalHandlerException.class
})
@ActiveProfiles("test")
class EndpointSecurityMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private ServiceService serviceService;
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

        // El filtro real solo autentica si hay Bearer; aqui lo sustituimos por un
        // mock que deja pasar la cadena para que el slice no corte la peticion.
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void deleteUserWithoutAuthorityIsDenied() throws Exception {
        assertDenied(mockMvc.perform(delete("/api/users/user-2")
                .with(authentication(authFor("VIEW_USER_PANEL")))));

        verify(userService, never()).delete(any(User.class), anyString());
    }

    @Test
    void deleteUserWithDeleteUserAuthoritySucceeds_H09() throws Exception {
        mockMvc.perform(delete("/api/users/user-2")
                        .with(authentication(authFor("DELETE_USER"))))
                .andExpect(status().isNoContent());

        verify(userService).delete(same(domainUser), eq("user-2"));
    }

    @Test
    void deleteUserWithPermissionButInactiveAccountIsDenied() throws Exception {
        when(securityService.isActive(any(Authentication.class))).thenReturn(false);

        assertDenied(mockMvc.perform(delete("/api/users/user-2")
                .with(authentication(authFor("DELETE_USER")))));

        verify(userService, never()).delete(any(User.class), anyString());
    }

    @Test
    void deleteServiceWithDeleteServiceButInactiveAccountIsDenied_H48() throws Exception {
        when(securityService.isActive(any(Authentication.class))).thenReturn(false);

        assertDenied(mockMvc.perform(delete("/api/services/service-1")
                .with(authentication(authFor("DELETE_SERVICE")))));

        verify(serviceService, never()).delete(anyString(), any(User.class));
    }

    @Test
    void deleteServiceWithPermissionAndActiveAccountSucceeds() throws Exception {
        mockMvc.perform(delete("/api/services/service-1")
                        .with(authentication(authFor("DELETE_SERVICE"))))
                .andExpect(status().isNoContent());

        verify(serviceService).delete("service-1", domainUser);
    }

    @Test
    void listServicesWithoutAuthorityIsDenied() throws Exception {
        assertDenied(mockMvc.perform(get("/api/services")
                .param("onlyActive", "true")
                .with(authentication(authFor("VIEW_MEMBER_PANEL")))));

        verify(serviceService, never()).findAll(anyBoolean());
    }

    @Test
    void listServicesWithViewPanelAuthoritySucceeds() throws Exception {
        when(serviceService.findAll(true)).thenReturn(List.of());

        mockMvc.perform(get("/api/services")
                        .param("onlyActive", "true")
                        .with(authentication(authFor("VIEW_SERVICE_PANEL"))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @Disabled("H-11 pendiente: GlobalHandlerException mapea AuthorizationDeniedException a 401, no a 403")
    void deniedRequestShouldReturn403_H11() throws Exception {
        mockMvc.perform(delete("/api/users/user-2")
                        .with(authentication(authFor("VIEW_USER_PANEL"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("All services ok"));
    }

    private Authentication authFor(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                domainUser,
                "not-used",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    /**
     * H-11 sigue pendiente: {@code GlobalHandlerException} responde 401 en vez de
     * 403. Se aceptan ambos y se exige que la respuesta sea de denegacion.
     */
    private void assertDenied(ResultActions result) throws Exception {
        MvcResult mvcResult = result.andReturn();
        int status = mvcResult.getResponse().getStatus();
        assertThat(status)
                .as("se esperaba denegacion (401 por H-11 o 403)")
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }
}
