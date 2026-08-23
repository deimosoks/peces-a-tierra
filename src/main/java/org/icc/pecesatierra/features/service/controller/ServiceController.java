package org.icc.pecesatierra.features.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.utils.models.BaseController;
import org.icc.pecesatierra.features.service.dtos.ServiceRequestDto;
import org.icc.pecesatierra.features.service.dtos.ServiceResponseDto;
import org.icc.pecesatierra.features.service.service.ServiceService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/services")
public class ServiceController extends BaseController {

    private final ServiceService serviceService;

    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).CREATE_SERVICE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    @PostMapping
    public ResponseEntity<ServiceResponseDto> create(@Valid @RequestBody ServiceRequestDto serviceRequestDto,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(serviceRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).UPDATE_SERVICE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
             )
             && 
             @securityService.isActive(authentication)""")
    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDto> update(@Valid @RequestBody ServiceRequestDto serviceRequestDto,
                                                     @PathVariable String serviceId,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(serviceService.update(serviceRequestDto, serviceId, user));
    }

    @PreAuthorize("""
    hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).DELETE_SERVICE.name()) || 
    hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) && 
    @securityService.isActive(authentication)
    """)
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDto> delete(@PathVariable String serviceId,
                                                     @AuthenticationPrincipal User user) {
        serviceService.delete(serviceId, user);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("""
            (hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).VIEW_SERVICE_PANEL.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).REGISTER_ATTENDANCE.name()) 
            || hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            )
            && 
            @securityService.isActive(authentication)""")
    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> findAll(@RequestParam boolean onlyActive) {
        return ResponseEntity.ok(serviceService.findAll(onlyActive));
    }

    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).UPDATE_SERVICE.name()) 
            ||
             hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
             ) 
             && 
             @securityService.isActive(authentication)""")
    @PatchMapping("/{serviceId}")
    public ResponseEntity<Boolean> updateState(@PathVariable String serviceId,
                                               @RequestParam boolean active,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(serviceService.updateActive(serviceId, active, user));
    }
}