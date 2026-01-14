package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.web.services.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/members")
public class MemberController extends BaseController {

    private final MemberService memberService;

    @PreAuthorize("hasAuthority('CREATE_MEMBER')")
    @PostMapping
    public ResponseEntity<MemberResponseDto> create(@ModelAttribute @Valid MemberRequestDto memberRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(memberRequestDto));
    }

    @PreAuthorize("hasAuthority('UPDATE_MEMBER')")
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> update(@Valid @ModelAttribute MemberRequestDto memberRequestDto,
                                                    @PathVariable String memberId) {
        return ResponseEntity.ok(memberService.update(memberRequestDto, memberId));
    }

    @PreAuthorize("hasAuthority('VIEW_MEMBER_PANEL') || hasAuthority('MANAGE_ATTENDANCE')")
    @GetMapping
    public ResponseEntity<MemberPagesResponseDto> findAll(@RequestParam(
                                                                  required = false,
                                                                  defaultValue = "0"
                                                          ) int page,
                                                          @RequestParam(
                                                                  required = false,
                                                                  defaultValue = "false"
                                                          ) boolean onlyActive,
                                                          @RequestParam(
                                                                  required = false,
                                                                  defaultValue = ""
                                                          ) String query) {
        return ResponseEntity.ok(memberService.findAll(page, onlyActive, query));
    }

    @PreAuthorize("hasAuthority('DELETE_MEMBER')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> delete(@PathVariable String memberId) {
        memberService.delete(memberId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('UPDATE_MEMBER')")
    @PatchMapping("/{memberId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String memberId,
                                                @RequestParam boolean active) {
        return ResponseEntity.ok(memberService.updateActive(memberId, active));
    }


}
