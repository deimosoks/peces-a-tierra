package org.icc.pecesatierra.features.baptism.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.baptism.dtos.BaptismFilterRequestDto;
import org.icc.pecesatierra.features.baptism.dtos.BaptismInvalidRequestDto;
import org.icc.pecesatierra.features.baptism.dtos.BaptismRequestDto;
import org.icc.pecesatierra.features.baptism.dtos.BaptismResponseDto;
import org.icc.pecesatierra.features.baptism.service.BaptismService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).BAPTISM_CREATE.name()) 
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).BAPTISM_INVALIDATE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<BaptismResponseDto> invalidate(@Valid @RequestBody BaptismInvalidRequestDto baptismInvalidRequestDto,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(baptismService.invalid(baptismInvalidRequestDto, user));
    }

    @PostMapping("/search")
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).VIEW_BAPTISM_PANEL.name()) 
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<PagesResponseDto<BaptismResponseDto>> search(@Valid @RequestBody BaptismFilterRequestDto baptismFilterRequestDto,
                                                                       @RequestParam int page,
                                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(baptismService.search(page, baptismFilterRequestDto, user));
    }
}
