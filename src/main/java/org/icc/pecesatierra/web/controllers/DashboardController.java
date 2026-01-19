package org.icc.pecesatierra.web.controllers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.web.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards")
@AllArgsConstructor
public class DashboardController extends BaseController  {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAuthority('MANAGE_DASHBOARD') && @securityService.isActive(authentication)")
    @PostMapping
    public ResponseEntity<DashboardResponseDto> dashboard() {
        return ResponseEntity.ok(dashboardService.dashboard());
    }
}
