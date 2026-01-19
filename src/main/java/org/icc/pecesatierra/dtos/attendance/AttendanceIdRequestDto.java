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
public class AttendanceIdRequestDto {
    @NotBlank(message = "Id de servicio invalido.")
    private String serviceId;
    @NotBlank(message = "Id de integrante invalida.")
    private String memberId;
    @NotNull(message = "Fecha de servicio invalida.")
    private LocalDateTime serviceDate;
}
