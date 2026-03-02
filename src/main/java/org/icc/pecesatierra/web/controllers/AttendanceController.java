package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.*;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.web.services.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendances")
public class AttendanceController extends BaseController {

    private final AttendanceService attendanceService;

    @PreAuthorize("""
            (
            hasAuthority('REGISTER_ATTENDANCE') 
            || 
            hasAuthority('ADMINISTRATOR') 
            ) 
            && 
            @securityService.isActive(authentication)
            """)
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid List<AttendanceRequestDto> attendances,
                                       @AuthenticationPrincipal User user) {
        attendanceService.create(attendances, user);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("""
            (
            hasAuthority('MANAGE_ATTENDANCE') 
            || 
            hasAuthority('ADMINISTRATOR')
            )
            && 
            @securityService.isActive(authentication)
            """)
    @PostMapping("/search")
    public ResponseEntity<PagesResponseDto<AttendanceResponseDto>> search(@RequestBody(
                                                                                  required = false
                                                                          ) AttendanceFiltersRequestDto attendanceFiltersRequestDto,
                                                                          @RequestParam(
                                                                                  required = false,
                                                                                  defaultValue = "0"
                                                                          ) int page,
                                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attendanceService.search(page, attendanceFiltersRequestDto, user));
    }

    @PostMapping("/export")
    public ResponseEntity<ExportResponseDto<AttendanceResponseDto>> export(@RequestBody(
                                                                                   required = false
                                                                           ) AttendanceFiltersRequestDto dto,
                                                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attendanceService.export(dto, user));
    }

    @PreAuthorize("""
            (
            hasAuthority('MANAGE_ATTENDANCE') 
            || 
            hasAuthority('ADMINISTRATOR')
             )
             && 
            @securityService.isActive(authentication)
            """)
    @PatchMapping("/invalidate")
    public ResponseEntity<AttendanceResponseDto> invalidate(@RequestBody @Valid AttendanceInvalidRequestDto attendanceInvalidRequestDto,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attendanceService.invalidate(attendanceInvalidRequestDto, user));
    }


}
