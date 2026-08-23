package org.icc.pecesatierra.features.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.utils.models.BaseController;
import org.icc.pecesatierra.features.dashboard.dtos.DashboardResponseDto;
import org.icc.pecesatierra.features.dashboard.service.DashboardService;
import org.icc.pecesatierra.features.user.User;
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).MANAGE_DASHBOARD.name())
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)""")
    public ResponseEntity<DashboardResponseDto> dashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.dashboard(user));
    }
}
