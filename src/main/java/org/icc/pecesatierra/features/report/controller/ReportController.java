package org.icc.pecesatierra.features.report.controller;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.utils.models.BaseController;
import org.icc.pecesatierra.features.report.dtos.ReportRequestDto;
import org.icc.pecesatierra.features.report.dtos.ReportResponseDto;
import org.icc.pecesatierra.features.report.service.ReportService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController extends BaseController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).MANAGE_REPORT.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).MANAGE_DASHBOARD.name())
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<List<ReportResponseDto>> generate(@RequestBody ReportRequestDto reportRequestDto,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generate(reportRequestDto, user));
    }
}
