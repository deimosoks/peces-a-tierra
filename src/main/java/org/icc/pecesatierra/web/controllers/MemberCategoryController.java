package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.MemberCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member-category")
@RequiredArgsConstructor
public class MemberCategoryController {

    private final MemberCategoryService memberCategoryService;

    @PostMapping
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<MemberCategoryResponseDto> create(@Valid @RequestBody MemberCategoryRequestDto memberCategoryRequestDto,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberCategoryService.create(memberCategoryRequestDto, user));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<MemberCategoryResponseDto> update(@Valid @RequestBody MemberCategoryRequestDto memberCategoryRequestDto,
                                                            @PathVariable String categoryId,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberCategoryService.update(memberCategoryRequestDto, categoryId, user));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String categoryId,
                                       @AuthenticationPrincipal User user) {
        memberCategoryService.delete(categoryId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@securityService.isActive(authentication)")
    public ResponseEntity<List<MemberCategoryResponseDto>> findAll() {
        return ResponseEntity.ok(memberCategoryService.findAll());
    }

}
