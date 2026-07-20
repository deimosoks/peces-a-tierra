package org.icc.pecesatierra.features.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.utils.models.BaseController;
import org.icc.pecesatierra.features.member.dtos.MemberExportDto;
import org.icc.pecesatierra.features.member.dtos.MemberFilterRequestDto;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.member.service.MemberService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController extends BaseController {

    private final MemberService memberService;

    @PreAuthorize("""
            (
            hasAuthority('CREATE_MEMBER') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    @PostMapping
    public ResponseEntity<MemberResponseDto> create(@ModelAttribute @Valid MemberRequestDto memberRequestDto,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(memberRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_MEMBER') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)""")
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> update(@Valid @ModelAttribute MemberRequestDto memberRequestDto,
                                                    @PathVariable String memberId,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.update(memberRequestDto, memberId, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('VIEW_MEMBER_PANEL') || 
            hasAuthority('VIEW_BAPTISM_PANEL') || 
            hasAuthority('MANAGE_ATTENDANCE') || 
            hasAuthority('REGISTER_ATTENDANCE') ||
            hasAuthority('DISCIPULADO_PROGRESS') ||
            hasAuthority('DISCIPULADO_CREATE') || 
            hasAuthority('ADMINISTRATOR') 
             ) && 
            @securityService.isActive(authentication)
            """)
    @PostMapping("/search")
    public ResponseEntity<PagesResponseDto<MemberResponseDto>> search(@RequestBody(
                                                                              required = false
                                                                      ) MemberFilterRequestDto memberFilterRequestDto,
                                                                      @RequestParam(
                                                                              required = false,
                                                                              defaultValue = "0"
                                                                      ) int page,
                                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.search(page, memberFilterRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('VIEW_MEMBER_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    @PostMapping("/export")
    public ResponseEntity<ExportResponseDto<MemberExportDto>> export(@RequestBody MemberFilterRequestDto memberFilterRequestDto,
                                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.export(memberFilterRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('DELETE_MEMBER') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)""")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> delete(@PathVariable String memberId, @AuthenticationPrincipal User user) {
        memberService.delete(memberId, user);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_MEMBER') 
            || 
            hasAuthority('ADMINISTRATOR')
            )
            &&
            @securityService.isActive(authentication)
            """)
    @PatchMapping("/{memberId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String memberId,
                                                @RequestParam boolean active,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.updateActive(memberId, active, user));
    }

}
