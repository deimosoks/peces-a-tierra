package org.icc.pecesatierra.dtos.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequestDto {

    @NotBlank
    private String serviceId;

    @NotBlank
    private String memberId;

    @NotNull
    private LocalDateTime attendanceDate;

}
