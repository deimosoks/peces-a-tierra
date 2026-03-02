package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.baptism.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.web.services.BaptismService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/baptisms")
@RequiredArgsConstructor
public class BaptismController {

    private final BaptismService baptismService;

    @PostMapping
    @PreAuthorize("""
            (
            hasAuthority('BAPTISM_CREATE') 
            ||
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<BaptismResponseDto> create(@Valid @ModelAttribute BaptismRequestDto baptismRequestDto,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baptismService.create(baptismRequestDto, user));
    }

    @PostMapping("/invalidate")
    @PreAuthorize("""
            (
            hasAuthority('BAPTISM_INVALIDATE') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    public ResponseEntity<BaptismResponseDto> invalidate(@Valid @RequestBody BaptismInvalidRequestDto baptismInvalidRequestDto,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(baptismService.invalid(baptismInvalidRequestDto, user));
    }

    @PostMapping("/search")
    @PreAuthorize("""
            (
            hasAuthority('VIEW_BAPTISM_PANEL') 
            ||
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    public ResponseEntity<PagesResponseDto<BaptismResponseDto>> search(@Valid @RequestBody BaptismFilterRequestDto baptismFilterRequestDto,
                                                                       @RequestParam int page,
                                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(baptismService.search(page, baptismFilterRequestDto, user));
    }
}
