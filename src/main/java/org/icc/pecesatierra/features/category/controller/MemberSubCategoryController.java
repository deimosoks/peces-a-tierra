package org.icc.pecesatierra.features.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.features.category.service.MemberSubCategoryService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member-sub-category")
public class MemberSubCategoryController {

    private final MemberSubCategoryService memberSubCategoryService;

    @PostMapping
    @PreAuthorize("""
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<MemberSubCategoryResponseDto> create(@Valid @RequestBody MemberSubCategoryRequestDto memberSubCategoryRequestDto,
                                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberSubCategoryService.create(memberSubCategoryRequestDto, user));
    }

    @PutMapping("/{subCategoryId}")
    @PreAuthorize("""
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<MemberSubCategoryResponseDto> update(@Valid @RequestBody MemberSubCategoryRequestDto memberSubCategoryRequestDto,
                                                               @PathVariable String subCategoryId,
                                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberSubCategoryService.update(memberSubCategoryRequestDto, subCategoryId, user));
    }

    @DeleteMapping("/{subCategoryId}")
    @PreAuthorize("""
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String subCategoryId,
                                       @AuthenticationPrincipal User user) {
        memberSubCategoryService.delete(subCategoryId, user);
        return ResponseEntity.noContent().build();
    }

}
