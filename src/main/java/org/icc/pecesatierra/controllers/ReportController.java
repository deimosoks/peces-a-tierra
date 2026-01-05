package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.report.AttendanceReportRowDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportSummaryDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReportController {
    ResponseEntity<List<AttendanceReportRowDto>> generate(ReportRequestDto reportRequestDto);
}
