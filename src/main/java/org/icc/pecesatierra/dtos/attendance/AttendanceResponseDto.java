package org.icc.pecesatierra.dtos.attendance;

import lombok.*;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.dtos.member.type.MemberTypeResponseDto;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDto {

    private String id;
    private String serviceName;
    private String memberCompleteName;
    private MemberCategoryResponseDto memberCategory;
    private MemberTypeResponseDto memberType;
    private LocalDateTime serviceDate;
    private LocalDateTime attendanceDate;
    private String branchName;
    private boolean invalid;
    private String note;
    private String registeredBy;
    private String invalidReason;
    private LocalDateTime invalidAt;
    private String invalidatedBy;
    private MemberSubCategoryResponseDto subCategory;

}
