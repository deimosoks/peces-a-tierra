package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.report.*;

import java.util.List;

public interface ReportService {
    List<AttendanceReportRowDto> generate(ReportRequestDto reportRequestDto);
}
