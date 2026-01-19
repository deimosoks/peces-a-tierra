package org.icc.pecesatierra.dtos.service;

import jakarta.validation.constraints.NotBlank;
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
    private String description;

}
