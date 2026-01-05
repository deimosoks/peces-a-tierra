package org.icc.pecesatierra.controllers.imp;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.AttendanceController;
import org.icc.pecesatierra.dtos.attendance.AttendanceRequestDto;
import org.icc.pecesatierra.services.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/attendances")
public class AttendanceControllerImp implements AttendanceController {

    private final AttendanceService attendanceService;

    @Override
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody List<AttendanceRequestDto> attendances) {
        attendanceService.create(attendances);
        return ResponseEntity.ok().build();
    }
}
