package org.icc.pecesatierra.controllers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.AttendanceRequestDto;
import org.icc.pecesatierra.services.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/attendances")
public class AttendanceController extends BaseController {

    private final AttendanceService attendanceService;

    @PreAuthorize("hasAuthority('MANAGE_ATTENDANCE')")
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody List<AttendanceRequestDto> attendances) {
        attendanceService.create(attendances);
        return ResponseEntity.ok().build();
    }
}
