package org.icc.pecesatierra.web.controllers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.web.services.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@AllArgsConstructor
public class ReportController extends BaseController  {

    private final ReportService reportService;

    @PreAuthorize("hasAuthority('MANAGE_REPORT')")
    @PostMapping
    public ResponseEntity<List<ReportResponseDto>> generate(@RequestBody ReportRequestDto reportRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generate(reportRequestDto));
    }
}

//List<String> typePeople
//List<String> category
//List<Long> serviceIds;
//LocalDateTime startDate
//LocalDateTime endDate
//String userId