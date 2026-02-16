package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.attendance.*;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface AttendanceService {
    void create(List<AttendanceRequestDto> attendances, User user);
    AttendanceResponseDto invalidate(AttendanceInvalidRequestDto attendanceInvalidRequestDto, User user);
    AttendancePagesResponseDto findAll(int page, AttendanceFiltersRequestDto attendanceFiltersRequestDto, User user);
}
