package org.icc.pecesatierra.web.controllers;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    @PostMapping
    @PreAuthorize("""
            (
            hasAuthority('MANAGE_DASHBOARD') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    public ResponseEntity<DashboardResponseDto> dashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.dashboard(user));
    }
}
