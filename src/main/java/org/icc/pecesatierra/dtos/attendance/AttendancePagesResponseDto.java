package org.icc.pecesatierra.dtos.attendance;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePagesResponseDto {
    private List<AttendanceResponseDto> attendances;
    private int pages;
}
