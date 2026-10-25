package org.icc.pecesatierra.features.member.controller;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.features.member.service.MemberNoteService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/members")
@RestController
@RequiredArgsConstructor
public class MemberNoteController {

    private final MemberNoteService memberNoteService;

    @PreAuthorize("""
            (
            hasAuthority('CREATE_NOTE') 
            || 
            hasAuthority('ADMINISTRATOR')
            )
            &&
            @securityService.isActive(authentication)
            """)
    @PostMapping("/notes")
    public ResponseEntity<MemberNoteResponseDto> createNote(@Validated({Default.class, MemberNoteRequestDto.CreateValidation.class})
                                                            @RequestBody MemberNoteRequestDto memberNoteRequestDto,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberNoteService.createNote(memberNoteRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('DELETE_NOTE') 
            || 
            hasAuthority('ADMINISTRATOR')
            )
            &&
            @securityService.isActive(authentication)
            """)
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String noteId,
                                           @AuthenticationPrincipal User user) {
        memberNoteService.deleteNote(noteId, user);
        return ResponseEntity.ok().build();
    }

    /**
     * Notas de un integrante. Se apoya en VIEW_MEMBER_PANEL porque las notas se
     * muestran en el panel de integrantes; no existe un permiso especifico de
     * lectura de notas.
     */
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).VIEW_MEMBER_PANEL.name())
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            )
            &&
            @securityService.isActive(authentication)
            """)
    @GetMapping("/{memberId}/notes")
    public ResponseEntity<List<MemberNoteResponseDto>> findByMember(@PathVariable String memberId,
                                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberNoteService.findByMember(memberId, user));
    }

    /**
     * Edicion del contenido de una nota. No existe UPDATE_NOTE: el permiso de
     * escritura de notas es CREATE_NOTE, el mismo que autoriza crearlas.
     */
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).CREATE_NOTE.name())
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            )
            &&
            @securityService.isActive(authentication)
            """)
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<MemberNoteResponseDto> updateNote(@PathVariable String noteId,
                                                            @Valid @RequestBody MemberNoteRequestDto memberNoteRequestDto,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberNoteService.updateNote(noteId, memberNoteRequestDto, user));
    }

}
