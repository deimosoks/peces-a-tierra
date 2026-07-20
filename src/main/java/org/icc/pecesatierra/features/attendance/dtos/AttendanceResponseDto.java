package org.icc.pecesatierra.features.attendance.dtos;

import lombok.*;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryResponseDto;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.features.type.dtos.MemberTypeResponseDto;

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
