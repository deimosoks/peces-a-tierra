package org.icc.pecesatierra.utils.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.AttendanceIdResponseDto;
import org.icc.pecesatierra.dtos.attendance.AttendanceResponseDto;
import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class AttendanceMapper {

    private final MemberRepository memberRepository;

    public AttendanceResponseDto toDto(Attendance attendance) {

        return AttendanceResponseDto.builder()
                .id(
                        AttendanceIdResponseDto.builder()
                                .memberId(attendance.getId().getMemberId())
                                .serviceId(attendance.getId().getServiceId())
                                .serviceDate(attendance.getId().getServiceDate())
                                .build())
                .serviceName(attendance.getServices().getName())
                .memberCompleteName(attendance.getMember().getCompleteName())
                .memberCategory(attendance.getMemberCategory())
                .memberType(attendance.getMemberType())
                .attendanceDate(attendance.getAttendanceDate())
                .invalid(attendance.isInvalid())
                .note(attendance.getNote())
                .registeredBy(attendance.getRegisteredById() != null ? attendance.getRegisteredById().getCompleteName() : "desconocido.")
                .invalidReason(attendance.getInvalidReason())
                .invalidAt(attendance.getInvalidAt())
                .invalidatedBy(attendance.getInvalidatorId() != null ? attendance.getInvalidatorId().getCompleteName() : "desconocido.")
                .build();
    }

}
