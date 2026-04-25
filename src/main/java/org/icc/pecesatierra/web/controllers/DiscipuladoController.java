package org.icc.pecesatierra.web.controllers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.discipulado.DiscipuladoFilterRequestDto;
import org.icc.pecesatierra.dtos.discipulado.DiscipuladoRequestDto;
import org.icc.pecesatierra.dtos.discipulado.DiscipuladoResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.web.services.DiscipuladoService;
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
    public ResponseEntity<DiscipuladoResponseDto> findById(@PathVariable String id) {
        return ResponseEntity.ok(discipuladoService.findById(id));
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
                                                                           @RequestParam(required = false, defaultValue = "0") int page) {
        return ResponseEntity.ok(discipuladoService.search(page, filters, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("""
            (
            hasAuthority('DELETE_DISCIPULADO')
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String id) {
        discipuladoService.delete(id);
        return ResponseEntity.noContent().build();
    }



}
