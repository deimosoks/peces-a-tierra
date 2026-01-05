package org.icc.pecesatierra.services.imp;

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
import org.icc.pecesatierra.utils.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.icc.pecesatierra.utils.Validator.validate;

@Service
@AllArgsConstructor
public class AttendanceServiceImp implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ServiceRepository serviceRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public void create(List<AttendanceRequestDto> attendances) {

        attendances.forEach(attendanceRequestDto -> {

            Services service = serviceRepository.findById(attendanceRequestDto.getServiceId())
                    .orElseThrow(() -> new ServicesNotFoundException("Service doesn't exist."));

            validate(
                    service.isActive(),
                    new ServicesNotFoundException("You cannot create an attendance record with a service that is deactivated.")
            );

            Member member = memberRepository.findById(attendanceRequestDto.getMemberId())
                    .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist."));

            validate(
                    service.isActive(),
                    new ServicesNotFoundException("You cannot create an attendance record with a member that is deactivated.")
            );

            AttendanceId attendanceId = AttendanceId.builder()
                    .serviceId(service.getId())
                    .memberId(member.getId())
                    .attendanceDate(attendanceRequestDto.getAttendanceDate())
                    .build();

            Attendance attendance = Attendance.builder()
                    .id(attendanceId)
                    .member(member)
                    .services(service)
                    .memberCategory(member.getCategory())
                    .memberType(member.getType())
                    .build();

            attendanceRepository.save(attendance);
        });
    }
}
