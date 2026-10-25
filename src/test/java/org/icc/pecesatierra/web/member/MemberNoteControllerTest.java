package org.icc.pecesatierra.web.member;

import jakarta.servlet.FilterChain;
import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.configurations.JwtAuthFilter;
import org.icc.pecesatierra.configurations.SecurityConfig;
import org.icc.pecesatierra.configurations.SecurityService;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.category.repository.mapper.MemberNotesMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.MemberNotes;
import org.icc.pecesatierra.features.member.controller.MemberNoteController;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.features.member.repository.MemberNotesRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.member.service.MemberNoteService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de notas que consume el frontend. El slice MVC usa
 * el {@link MemberNoteService} real con repositorios y mapper mockeados, de modo
 * que los casos entre sedes prueban el scoping de verdad y no solo el mapeo de
 * una excepcion.
 *
 * <p>El scoping es el mismo de {@code DELETE /api/members/notes/{noteId}}
 * (el delete responde 409 con {@code CannotDeleteMemberOutSideYourBranchException});
 * aqui se usa una excepcion dedicada con el mismo estado para no reutilizar el
 * mensaje de "eliminar".</p>
 */
@WebMvcTest
@ContextConfiguration(classes = {
        MemberNoteController.class,
        MemberNoteService.class,
        SecurityConfig.class,
        GlobalHandlerException.class
})
@ActiveProfiles("test")
class MemberNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberNotesRepository memberNotesRepository;
    @MockitoBean
    private MemberRepository memberRepository;
    @MockitoBean
    private MemberNotesMapper memberNotesMapper;
    @MockitoBean
    private DateTimeUtils dateTimeUtils;
    @MockitoBean(name = "securityService")
    private SecurityService securityService;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");
    private final Member memberA = TestFixtures.member("member-a", branchA, "Ana A");
    private final Member memberB = TestFixtures.member("member-b", branchB, "Beto B");

    private User operatorA;

    @BeforeEach
    void setUp() throws Exception {
        operatorA = TestFixtures.user("user-1", "operador-a", memberA);
        when(securityService.isActive(any(Authentication.class))).thenReturn(true);
        when(memberNotesMapper.toDto(any(MemberNotes.class)))
                .thenAnswer(invocation -> toDto(invocation.getArgument(0)));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void getNotesWithViewMemberPanelReturns200() throws Exception {
        when(memberRepository.findById(memberA.getId())).thenReturn(Optional.of(memberA));
        when(memberNotesRepository.findByMember_IdOrderByCreatedAtDesc(memberA.getId()))
                .thenReturn(List.of(note("note-1", memberA, "Primera nota")));

        mockMvc.perform(get("/api/members/member-a/notes")
                        .with(authentication(authFor("VIEW_MEMBER_PANEL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("note-1"))
                .andExpect(jsonPath("$[0].note").value("Primera nota"));
    }

    @Test
    void getNotesWithoutViewPermissionIs403() throws Exception {
        mockMvc.perform(get("/api/members/member-a/notes")
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isForbidden());

        verify(memberRepository, never()).findById(anyString());
    }

    @Test
    void getNotesOfMemberInAnotherBranchIsDenied() throws Exception {
        when(memberRepository.findById(memberB.getId())).thenReturn(Optional.of(memberB));

        mockMvc.perform(get("/api/members/member-b/notes")
                        .with(authentication(authFor("VIEW_MEMBER_PANEL"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puede gestionar notas de integrantes fuera de su sede."));

        verify(memberNotesRepository, never()).findByMember_IdOrderByCreatedAtDesc(anyString());
    }

    @Test
    void getNotesOfMissingMemberIs404() throws Exception {
        when(memberRepository.findById("member-x")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/members/member-x/notes")
                        .with(authentication(authFor("VIEW_MEMBER_PANEL"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createNoteWithoutMemberIdIs400() throws Exception {
        mockMvc.perform(post("/api/members/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Nueva nota"
                                }
                                """)
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isBadRequest());

        verify(memberRepository, never()).findById(anyString());
    }

    @Test
    void createNoteWithMemberIdReturns200() throws Exception {
        when(memberRepository.findById(memberA.getId())).thenReturn(Optional.of(memberA));
        when(memberNotesRepository.save(any(MemberNotes.class))).thenAnswer(invocation -> {
            MemberNotes saved = invocation.getArgument(0);
            saved.setId("note-new");
            return saved;
        });

        mockMvc.perform(post("/api/members/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Nueva nota",
                                  "memberId": "member-a"
                                }
                                """)
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("note-new"))
                .andExpect(jsonPath("$.note").value("Nueva nota"));
    }

    @Test
    void updateNoteWithCreateNotePermissionReturns200() throws Exception {
        MemberNotes note = note("note-1", memberA, "Contenido anterior");
        when(memberNotesRepository.findById(note.getId())).thenReturn(Optional.of(note));

        mockMvc.perform(put("/api/members/notes/note-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Contenido actualizado"
                                }
                                """)
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("note-1"))
                .andExpect(jsonPath("$.note").value("Contenido actualizado"));

        verify(memberNotesRepository).save(note);
    }

    @Test
    void updateNoteWithoutCreateNotePermissionIs403() throws Exception {
        mockMvc.perform(put("/api/members/notes/note-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Contenido actualizado"
                                }
                                """)
                        .with(authentication(authFor("VIEW_MEMBER_PANEL"))))
                .andExpect(status().isForbidden());

        verify(memberNotesRepository, never()).findById(anyString());
    }

    @Test
    void updateNoteWithBlankContentIs400() throws Exception {
        mockMvc.perform(put("/api/members/notes/note-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": ""
                                }
                                """)
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isBadRequest());

        verify(memberNotesRepository, never()).findById(anyString());
    }

    @Test
    void updateMissingNoteIs404() throws Exception {
        when(memberNotesRepository.findById("note-x")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/members/notes/note-x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Contenido actualizado"
                                }
                                """)
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nota no encontrada."));
    }

    @Test
    void updateNoteOfAnotherBranchIsDenied() throws Exception {
        MemberNotes foreignNote = note("note-b", memberB, "Nota de otra sede");
        when(memberNotesRepository.findById(foreignNote.getId())).thenReturn(Optional.of(foreignNote));

        mockMvc.perform(put("/api/members/notes/note-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Intento de edicion"
                                }
                                """)
                        .with(authentication(authFor("CREATE_NOTE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puede gestionar notas de integrantes fuera de su sede."));

        verify(memberNotesRepository, never()).save(any(MemberNotes.class));
    }

    @Test
    void administratorCanUpdateNoteOfAnotherBranch() throws Exception {
        User administrator = TestFixtures.user("user-admin", "admin-a", memberA, "ADMINISTRATOR");
        MemberNotes foreignNote = note("note-b", memberB, "Nota de otra sede");
        when(memberNotesRepository.findById(foreignNote.getId())).thenReturn(Optional.of(foreignNote));

        mockMvc.perform(put("/api/members/notes/note-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Edicion del administrador"
                                }
                                """)
                        .with(authentication(authenticationFor(administrator, "ADMINISTRATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Edicion del administrador"));

        verify(memberNotesRepository).save(foreignNote);
    }

    private MemberNotes note(String id, Member member, String content) {
        return MemberNotes.builder()
                .id(id)
                .note(content)
                .member(member)
                .createdBy(member)
                .createdAt(TestFixtures.FIXED_NOW)
                .build();
    }

    private MemberNoteResponseDto toDto(MemberNotes note) {
        return MemberNoteResponseDto.builder()
                .id(note.getId())
                .note(note.getNote())
                .createdBy(note.getCreatedBy().getCompleteName())
                .createdAt(TestFixtures.FIXED_NOW.toLocalDateTime())
                .build();
    }

    private Authentication authFor(String... authorities) {
        return authenticationFor(operatorA, authorities);
    }

    private Authentication authenticationFor(User principal, String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                "not-used",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}
