package org.icc.pecesatierra.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.AttendanceRequestDto;
import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.AttendanceId;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.ServicesNotFoundException;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.ServiceRepository;
import org.icc.pecesatierra.services.AttendanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ServiceRepository serviceRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public void create(List<AttendanceRequestDto> attendances) {

        attendances.forEach(attendanceRequestDto -> {

            Services service = serviceRepository.findById(attendanceRequestDto.getServiceId())
                    .orElseThrow(() -> new ServicesNotFoundException("Service doesn't exist."));

            if (!service.isActive())
                throw new ServicesNotFoundException("You cannot create an attendance record with a service that is deactivated.");

            Member member = memberRepository.findById(attendanceRequestDto.getMemberId())
                    .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist."));

            if (!member.isActive())
                throw new ServicesNotFoundException("You cannot create an attendance record with a member that is deactivated.");

            AttendanceId attendanceId = AttendanceId.builder()
                    .serviceId(service.getId())
                    .memberId(member.getId())
                    .attendanceDate(LocalDateTime.now())
                    .build();

            Attendance attendance = Attendance.builder()
                    .id(attendanceId)
                    .member(member)
                    .services(service)
                    .memberCategory(member.getCategory())
                    .memberType(member.getType())
                    .serviceStartDate(attendanceRequestDto.getAttendanceDate())
                    .build();

            attendanceRepository.save(attendance);
        });
    }
}
