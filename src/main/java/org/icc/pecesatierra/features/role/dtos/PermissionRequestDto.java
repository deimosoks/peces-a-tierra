package org.icc.pecesatierra.features.role.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.utils.enums.AppPermission;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionRequestDto {

    @NotNull(message = "Permiso invalido.")
    private AppPermission name;
}
