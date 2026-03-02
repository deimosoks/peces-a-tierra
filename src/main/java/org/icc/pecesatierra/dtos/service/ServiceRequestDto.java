package org.icc.pecesatierra.dtos.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDto {

    @NotBlank(message = "Nombre invalido.")
    private String name;

    @NotBlank(message = "Descripción invalida.")
    @Size(max = 255, message = "La descripción debe tener un maximo de 255 caracteres.")
    private String description;

}
