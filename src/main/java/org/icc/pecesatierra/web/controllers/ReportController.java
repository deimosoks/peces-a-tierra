package org.icc.pecesatierra.web.controllers;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.ReportService;
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
            hasAuthority('MANAGE_REPORT') 
            || 
            hasAuthority('ADMINISTRATOR')
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<List<ReportResponseDto>> generate(@RequestBody ReportRequestDto reportRequestDto,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generate(reportRequestDto, user));
    }
}
