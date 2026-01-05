package org.icc.pecesatierra.dtos.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionResponseDto {

    @NotBlank
    private String name;

}
