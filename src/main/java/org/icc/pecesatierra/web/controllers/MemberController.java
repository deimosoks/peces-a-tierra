package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.*;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/members")
public class MemberController extends BaseController {

    private final MemberService memberService;

    @PreAuthorize("hasAuthority('CREATE_MEMBER') || hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    @PostMapping
    public ResponseEntity<MemberResponseDto> create(@ModelAttribute @Valid MemberRequestDto memberRequestDto,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(memberRequestDto, user));
    }

    @PreAuthorize("hasAuthority('UPDATE_MEMBER') || hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> update(@Valid @ModelAttribute MemberRequestDto memberRequestDto,
                                                    @PathVariable String memberId,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.update(memberRequestDto, memberId, user));
    }

    @PreAuthorize("hasAuthority('VIEW_MEMBER_PANEL') || (hasAuthority('VIEW_BAPTISM_PANEL') || hasAuthority('MANAGE_ATTENDANCE') || hasAuthority('REGISTER_ATTENDANCE')) || hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    @PostMapping("/search")
    public ResponseEntity<MemberPagesResponseDto> findAll(@RequestBody(
                                                                  required = false
                                                          ) MemberFilterRequestDto memberFilterRequestDto,
                                                          @RequestParam(
                                                                  required = false,
                                                                  defaultValue = "0"
                                                          ) int page,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.findAll(page, memberFilterRequestDto, user));
    }

    @PreAuthorize("hasAuthority('VIEW_MEMBER_PANEL') || hasAuthority('ADMINISTRATOR')")
    @PostMapping("/export")
    public ResponseEntity<List<MemberExportDto>> findAllData(@RequestBody MemberFilterRequestDto memberFilterRequestDto,
                                                             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.findAllData(memberFilterRequestDto, user));
    }

    @PreAuthorize("hasAuthority('DELETE_MEMBER') || hasAuthority('ADMINISTRATOR')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> delete(@PathVariable String memberId, @AuthenticationPrincipal User user) {
        memberService.delete(memberId, user);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('UPDATE_MEMBER') || hasAuthority('ADMINISTRATOR')")
    @PatchMapping("/{memberId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String memberId,
                                                @RequestParam boolean active,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.updateActive(memberId, active, user));
    }

    @PreAuthorize("hasAuthority('CREATE_NOTE') || hasAuthority('ADMINISTRATOR')")
    @PostMapping("/notes")
    public ResponseEntity<MemberNoteResponseDto> createNote(@Valid @RequestBody MemberNoteRequestDto memberNoteRequestDto,
                                                            @AuthenticationPrincipal User user){
        return ResponseEntity.ok(memberService.createNote(memberNoteRequestDto, user));
    }

    @PreAuthorize("hasAuthority('DELETE_NOTE') || hasAuthority('ADMINISTRATOR')")
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String noteId,
                                           @AuthenticationPrincipal User user){
        memberService.deleteNote(noteId, user);
        return ResponseEntity.ok().build();
    }

}
