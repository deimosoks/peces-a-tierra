package org.icc.pecesatierra.features.branch.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.branch.dtos.BranchRequestDto;
import org.icc.pecesatierra.features.branch.dtos.BranchResponseDto;
import org.icc.pecesatierra.features.branch.service.BranchService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).BRANCH_CREATE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<BranchResponseDto> create(@Valid @RequestBody BranchRequestDto branchRequestDto,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(branchRequestDto, user));
    }

    @GetMapping
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).VIEW_BRANCH_PANEL.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            ) 
            &&
            @securityService.isActive(authentication)""")
    public ResponseEntity<List<BranchResponseDto>> findAll() {
        return ResponseEntity.ok(branchService.findAll());
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).BRANCH_UPDATE.name()) 
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<BranchResponseDto> update(@Valid @RequestBody BranchRequestDto branchRequestDto,
                                                    @PathVariable String branchId,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(branchService.update(branchRequestDto, branchId, user));
    }

    @DeleteMapping("/{branchId}")
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).BRANCH_DELETE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String branchId,
                                       @AuthenticationPrincipal User user) {
        branchService.delete(branchId, user);
        return ResponseEntity.noContent().build();
    }

}
