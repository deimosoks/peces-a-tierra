package org.icc.pecesatierra.dtos.baptism;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaptismInvalidRequestDto {

    @NotBlank
    private String baptismId;

    @NotBlank(message = "Debe ingresar una razón valida.")
    @Size(max = 255, message = "La razón debe tener un maximo de 255 caracteres.")
    private String invalidReason;

}
