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

    @NotBlank(message = "Id de servicio invalido.")
    private String serviceEventId;

    @NotBlank(message = "Id de miembro invalido.")
    private String memberId;

    @NotNull(message = "Fecha de servicio invalida.")
    private LocalDateTime serviceDate;

    @NotNull(message = "Fecha de llegada invalida.")
    private LocalDateTime attendanceDate;

    private String note;

}
