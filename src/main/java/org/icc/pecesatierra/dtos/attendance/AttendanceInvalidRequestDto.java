package org.icc.pecesatierra.dtos.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceInvalidRequestDto {

    @NotNull(message = "Id de asistencia es obligatorio.")
    private String attendanceId;

    @NotBlank(message = "Debe ingresar una razón valida.")
    @Size(max = 255, message = "La razón debe tener un maximo de 255 caracteres.")
    private String invalidReason;
}
