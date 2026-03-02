package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.report.ReportRequestDto;
import org.icc.pecesatierra.dtos.report.ReportResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface ReportService {
    List<ReportResponseDto> generate(ReportRequestDto dto, User user);
}
