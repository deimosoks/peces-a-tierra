package org.icc.pecesatierra.utils.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.attendance.AttendanceIdResponseDto;
import org.icc.pecesatierra.dtos.attendance.AttendanceResponseDto;
import org.icc.pecesatierra.entities.Attendance;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class AttendanceMapper {

    private final MemberTypeMapper memberTypeMapper;
    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberSubCategoryMapper memberSubCategoryMapper;

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
                .memberCategory(memberCategoryMapper.toDto(attendance.getMemberCategory()))
                .memberType(memberTypeMapper.toDto(attendance.getMemberType()))
                .subCategory(memberSubCategoryMapper.toDto(attendance.getMemberSubCategory()))
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
