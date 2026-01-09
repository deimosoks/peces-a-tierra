package org.icc.pecesatierra.controllers.imp;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.DashboardController;
import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards")
@AllArgsConstructor
public class DashboardControllerImp implements DashboardController {

    private final DashboardService dashboardService;

    @Override
    @PreAuthorize("hasAuthority('MANAGE_DASHBOARD')")
    @PostMapping
    public ResponseEntity<DashboardResponseDto> dashboard() {
        return ResponseEntity.ok(dashboardService.dashboard());
    }
}
