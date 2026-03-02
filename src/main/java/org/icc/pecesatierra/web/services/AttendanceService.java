package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.attendance.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;

import java.util.List;

public interface AttendanceService {
    void create(List<AttendanceRequestDto> attendances, User user);
    AttendanceResponseDto invalidate(AttendanceInvalidRequestDto attendanceInvalidRequestDto, User user);
    PagesResponseDto<AttendanceResponseDto> search(int page, AttendanceFiltersRequestDto attendanceFiltersRequestDto, User user);
    ExportResponseDto<AttendanceResponseDto> export(AttendanceFiltersRequestDto dto, User user);
}
