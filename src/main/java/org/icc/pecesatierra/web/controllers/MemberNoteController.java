package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.MemberNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/members/notes")
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
    @PostMapping()
    public ResponseEntity<MemberNoteResponseDto> createNote(@Valid @RequestBody MemberNoteRequestDto memberNoteRequestDto,
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
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String noteId,
                                           @AuthenticationPrincipal User user) {
        memberNoteService.deleteNote(noteId, user);
        return ResponseEntity.ok().build();
    }

}

