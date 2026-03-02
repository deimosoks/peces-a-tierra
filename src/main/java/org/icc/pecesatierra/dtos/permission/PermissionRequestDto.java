package org.icc.pecesatierra.dtos.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.icc.pecesatierra.utils.enums.AppPermission;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionRequestDto {

    @NotBlank(message = "Permiso invalido.")
    private AppPermission name;
}
