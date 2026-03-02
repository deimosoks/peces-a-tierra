package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.AttendanceResponseDto;
import org.icc.pecesatierra.entities.Attendance;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AttendanceMapper {

    private final MemberTypeMapper memberTypeMapper;
    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberSubCategoryMapper memberSubCategoryMapper;

    public AttendanceResponseDto toDto(Attendance attendance) {

        return AttendanceResponseDto.builder()
                .id(String.valueOf(attendance.getId()))
                .serviceName(attendance.getServiceEvent().getServices().getName())
                .memberCompleteName(attendance.getMember().getCompleteName())
                .memberCategory(memberCategoryMapper.toDto(attendance.getMemberCategory()))
                .memberType(memberTypeMapper.toDto(attendance.getMemberType()))
                .subCategory(memberSubCategoryMapper.toDto(attendance.getMemberSubCategory()))
                .serviceDate(attendance.getServiceEvent().getStartDateTime())
                .attendanceDate(attendance.getAttendanceDate())
                .branchName(attendance.getBranch().getName())
                .invalid(attendance.isInvalid())
                .note(attendance.getNote())
                .registeredBy(attendance.getRegisteredById() != null ? attendance.getRegisteredById().getCompleteName() : "desconocido.")
                .invalidReason(attendance.getInvalidReason())
                .invalidAt(attendance.getInvalidAt())
                .invalidatedBy(attendance.getInvalidatorId() != null ? attendance.getInvalidatorId().getCompleteName() : "desconocido.")
                .build();
    }

}
