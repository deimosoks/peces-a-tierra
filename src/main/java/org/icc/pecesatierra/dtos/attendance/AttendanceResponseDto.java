package org.icc.pecesatierra.dtos.attendance;

import lombok.*;

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
    private String memberCategory;
    private String memberType;
    private LocalDateTime attendanceDate;
    private boolean invalid;
    private String note;
    private String registeredBy;
    private String invalidReason;
    private LocalDateTime invalidAt;
    private String invalidatorId;

}
