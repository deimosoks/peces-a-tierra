package org.icc.pecesatierra.controllers.imp;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.ReportController;
import org.icc.pecesatierra.dtos.report.AttendanceReportRowDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.dtos.report.ReportSummaryDto;
import org.icc.pecesatierra.services.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@AllArgsConstructor
public class ReportControllerImp implements ReportController {

    private final ReportService reportService;

    @Override
    @PostMapping
    public ResponseEntity<List<AttendanceReportRowDto>> generate(@RequestBody ReportRequestDto reportRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generate(reportRequestDto));
    }
}

//List<String> typePeople
//List<String> category
//List<Long> serviceIds;
//LocalDateTime startDate
//LocalDateTime endDate
//String userId