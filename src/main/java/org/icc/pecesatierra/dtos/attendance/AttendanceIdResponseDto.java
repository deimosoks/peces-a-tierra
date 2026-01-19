package org.icc.pecesatierra.dtos.attendance;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceIdResponseDto {
    private String serviceId;
    private String memberId;
    private LocalDateTime serviceDate;
}
