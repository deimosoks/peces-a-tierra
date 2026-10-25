package org.icc.pecesatierra.web.member;

import jakarta.servlet.FilterChain;
import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.configurations.SecurityService;
import org.icc.pecesatierra.features.member.controller.MemberController;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.member.service.MemberService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de regresion de H-28: {@code MemberRequestDto.gender} no era
 * obligatorio, y {@code MemberPersistenceService} / {@code MemberMapper}
 * hacian {@code getGender().toString()}. Un genero nulo en el alta o en la
 * edicion respondia 500; ahora el binding responde 400 antes de llegar al
 * servicio.
 */
@WebMvcTest
@ContextConfiguration(classes = {
        MemberController.class,
        SecurityConfig.class,
        GlobalHandlerException.class
})
@ActiveProfiles("test")
class MemberControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;
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
                TestFixtures.member("op-member", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));
        when(securityService.isActive(any(Authentication.class))).thenReturn(true);

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void createMemberWithoutGenderIs400_H28() throws Exception {
        mockMvc.perform(multipart("/api/members")
                        .param("completeName", "Ana Perez")
                        .param("typeId", "type-1")
                        .param("categoryId", "cat-1")
                        .with(authentication(authFor("CREATE_MEMBER"))))
                .andExpect(status().isBadRequest());

        // Antes del fix llegaba al servicio y el NPE ocurria en getGender().toString().
        verify(memberService, never()).create(any(), any(User.class));
    }

    @Test
    void updateMemberWithoutGenderIs400_H28() throws Exception {
        mockMvc.perform(multipart("/api/members/member-1")
                        .param("completeName", "Ana Perez")
                        .param("typeId", "type-1")
                        .param("categoryId", "cat-1")
                        .with(authentication(authFor("UPDATE_MEMBER")))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).update(any(), anyString(), any(User.class));
    }

    @Test
    void createMemberWithGenderStillSucceeds_H28() throws Exception {
        when(memberService.create(any(), any(User.class)))
                .thenReturn(MemberResponseDto.builder().id("member-1").completeName("Ana Perez").build());

        mockMvc.perform(multipart("/api/members")
                        .param("completeName", "Ana Perez")
                        .param("typeId", "type-1")
                        .param("categoryId", "cat-1")
                        .param("gender", "HOMBRE")
                        .with(authentication(authFor("CREATE_MEMBER"))))
                .andExpect(status().isCreated());

        verify(memberService).create(any(), eq(domainUser));
    }

    private Authentication authFor(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                domainUser,
                "not-used",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}
