package org.icc.pecesatierra.dtos.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.entities.AttendanceId;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceInvalidRequestDto {

    @NotNull(message = "Id de asistencia es obligatorio.")
    private AttendanceIdRequestDto attendanceId;

    @NotBlank(message = "Debe ingresar una razón valida.")
    private String invalidReason;
}
