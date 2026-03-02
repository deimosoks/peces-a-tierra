package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.branch.BranchRequestDto;
import org.icc.pecesatierra.dtos.branch.BranchResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.BranchService;
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
            hasAuthority('ADMINISTRATOR') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    public ResponseEntity<BranchResponseDto> create(@Valid @RequestBody BranchRequestDto branchRequestDto,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(branchRequestDto, user));
    }

    @GetMapping
    @PreAuthorize("""
            (
            hasAuthority('VIEW_BRANCH_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR') 
            ) 
            &&
            @securityService.isActive(authentication)""")
    public ResponseEntity<List<BranchResponseDto>> findAll() {
        return ResponseEntity.ok(branchService.findAll());
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("""
            (
            hasAuthority('BRANCH_UPDATE') 
            ||
            hasAuthority('ADMINISTRATOR')
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
            hasAuthority('BRANCH_DELETE') 
            || 
            hasAuthority('ADMINISTRATOR')
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
