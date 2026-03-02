package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/roles")
@RequiredArgsConstructor
@RestController
public class RoleController extends BaseController {

    private final RoleService roleService;

    @PreAuthorize("""
            (
            hasAuthority('CREATE_ROLE') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    @PostMapping
    public ResponseEntity<RoleResponseDto> create(@Valid @RequestBody RoleRequestDto roleRequestDto,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(roleRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_ROLE') 
            ||
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> update(@Valid @RequestBody RoleRequestDto roleRequestDto,
                                                  @PathVariable String roleId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(roleService.update(roleRequestDto, roleId, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('DELETE_ROLE') 
            || 
            hasAuthority('ADMINISTRATOR')
            )
             && 
             @securityService.isActive(authentication)""")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable String roleId,
                                       @AuthenticationPrincipal User user) {
        roleService.delete(roleId, user);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("""
            (
            hasAuthority('VIEW_ROLE_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    @GetMapping
    public ResponseEntity<List<RoleResponseDto>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }
}
