package org.icc.pecesatierra.features.type.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.type.dtos.MemberTypeRequestDto;
import org.icc.pecesatierra.features.type.dtos.MemberTypeResponseDto;
import org.icc.pecesatierra.features.type.service.MemberTypeService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member-type")
@RequiredArgsConstructor
public class MemberTypeController {

    private final MemberTypeService memberTypeService;

    @PostMapping
    @PreAuthorize("""
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<MemberTypeResponseDto> create(@Valid @RequestBody MemberTypeRequestDto memberTypeRequestDto,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberTypeService.create(memberTypeRequestDto, user));
    }

    @PutMapping("/{typeId}")
    @PreAuthorize("""
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<MemberTypeResponseDto> update(@Valid @RequestBody MemberTypeRequestDto memberTypeRequestDto,
                                                        @PathVariable String typeId) {
        return ResponseEntity.ok(memberTypeService.update(memberTypeRequestDto, typeId));
    }

    @DeleteMapping("/{typeId}")
    @PreAuthorize("""
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String typeId,
                                       @AuthenticationPrincipal User user) {
        memberTypeService.delete(typeId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@securityService.isActive(authentication)")
    public ResponseEntity<List<MemberTypeResponseDto>> findAll() {
        return ResponseEntity.ok(memberTypeService.findAll());
    }

}
