package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.attendance.AttendanceRequestDto;

import java.util.List;

public interface AttendanceService {
    void create(List<AttendanceRequestDto> attendances);
}
