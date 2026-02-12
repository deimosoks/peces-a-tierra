package org.icc.pecesatierra.dtos.attendance;

import lombok.*;
import org.icc.pecesatierra.dtos.member.MemberCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.MemberTypeResponseDto;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberType;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDto {

    private AttendanceIdResponseDto id;
    private String serviceName;
    private String memberCompleteName;
    private MemberCategoryResponseDto memberCategory;
    private MemberTypeResponseDto memberType;
    private LocalDateTime attendanceDate;
    private boolean invalid;
    private String note;
    private String registeredBy;
    private String invalidReason;
    private LocalDateTime invalidAt;
    private String invalidatedBy;

}
