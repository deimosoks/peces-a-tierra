package org.icc.pecesatierra.dtos.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.icc.pecesatierra.dtos.permission.PermissionRequestDto;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDto {

    @NotBlank(message = "Nombre invalido.")
    private String name;

    @NotBlank(message = "Color invalido.")
    private String color;

    @NotNull(message = "Permisos invalidos.")
    private Set<PermissionRequestDto> permissions;

    @NotBlank(message = "Descripción invalida.")
    @Size(max = 255, message = "La descripción debe tener un maximo de 255 caracteres.")
    private String description;

}
