package org.icc.pecesatierra.controllers;

import jakarta.validation.Valid;
import org.icc.pecesatierra.dtos.attendance.AttendanceRequestDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AttendanceController {
    ResponseEntity<Void> create( @Valid List<AttendanceRequestDto> attendances);
}
