package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReportController {
    ResponseEntity<List<ReportResponseDto>> generate(ReportRequestDto reportRequestDto);
}
