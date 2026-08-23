package org.icc.pecesatierra.features.role.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.utils.models.BaseController;
import org.icc.pecesatierra.features.role.dtos.RoleRequestDto;
import org.icc.pecesatierra.features.role.dtos.RoleResponseDto;
import org.icc.pecesatierra.features.role.service.RoleService;
import org.icc.pecesatierra.features.user.User;
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).CREATE_ROLE.name())
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).UPDATE_ROLE.name()) 
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            )
            && 
            @securityService.isActive(authentication)
            """)
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> update(@Valid @RequestBody RoleRequestDto roleRequestDto,
                                                  @PathVariable String roleId,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(roleService.update(roleRequestDto, roleId, user));
    }

    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).DELETE_ROLE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).VIEW_ROLE_PANEL.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).UPDATE_USER.name()) 
            )
            && 
            @securityService.isActive(authentication)
            """)
    @GetMapping
    public ResponseEntity<List<RoleResponseDto>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }
}
