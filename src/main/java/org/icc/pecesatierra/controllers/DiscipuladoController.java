package org.icc.pecesatierra.controllers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.discipulado.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.services.DiscipuladoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discipulado")
@RequiredArgsConstructor
public class DiscipuladoController {

    private final DiscipuladoService discipuladoService;

    @PostMapping
    @PreAuthorize("""
            (
            hasAuthority('DISCIPULADO_CREATE') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<DiscipuladoResponseDto> create(@RequestBody DiscipuladoRequestDto dto,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discipuladoService.create(dto, user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
            (
            hasAuthority('VIEW_DISCIPULADO_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<DiscipuladoResponseDto> findById(@PathVariable String id,
                                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(discipuladoService.findById(id, user));
    }

    @PostMapping("/search")
    @PreAuthorize("""
            (
            hasAuthority('VIEW_DISCIPULADO_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<PagesResponseDto<DiscipuladoResponseDto>> search(@RequestBody DiscipuladoFilterRequestDto filters,
                                                                           @RequestParam(required = false, defaultValue = "0") int page,
                                                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(discipuladoService.search(page, filters, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("""
            (
            hasAuthority('DISCIPULADO_DELETE')
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal User user) {
        discipuladoService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/progress")
    @PreAuthorize("""
            (
            hasAuthority('DISCIPULADO_PROGRESS') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<DiscipuladoProgressResponseDto> progress(@RequestBody DiscipuladoProgressRequestDto dto) {
        return ResponseEntity.ok(discipuladoService.progress(dto, null));
    }

    @PutMapping("/progress/{id}")
    @PreAuthorize("""
            (
            hasAuthority('DISCIPULADO_PROGRESS') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<DiscipuladoProgressResponseDto> progressUpdate(@RequestBody DiscipuladoProgressRequestDto dto,
                                                                         @PathVariable String id,
                                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(discipuladoService.updateProgress(dto, user, id));
    }


}
