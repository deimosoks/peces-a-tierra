package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.ServiceService;
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
            hasAuthority('CREATE_SERVICE') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)""")
    @PostMapping
    public ResponseEntity<ServiceResponseDto> create(@Valid @RequestBody ServiceRequestDto serviceRequestDto,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(serviceRequestDto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_SERVICE') 
            || 
            hasAuthority('ADMINISTRATOR')
             )
             && 
             @securityService.isActive(authentication)""")
    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDto> update(@Valid @RequestBody ServiceRequestDto serviceRequestDto,
                                                     @PathVariable String serviceId,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(serviceService.update(serviceRequestDto, serviceId, user));
    }

    @PreAuthorize("hasAuthority('DELETE_SERVICE') || hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDto> delete(@PathVariable String serviceId,
                                                     @AuthenticationPrincipal User user) {
        serviceService.delete(serviceId, user);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("""
            (hasAuthority('VIEW_SERVICE_PANEL') 
            || 
            hasAuthority('REGISTER_ATTENDANCE') 
            || hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> findAll(@RequestParam boolean onlyActive) {
        return ResponseEntity.ok(serviceService.findAll(onlyActive));
    }

    @PreAuthorize("""
            (
            hasAuthority('UPDATE_SERVICE') 
            ||
             hasAuthority('ADMINISTRATOR')
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