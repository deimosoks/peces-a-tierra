package org.icc.pecesatierra.dtos.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.icc.pecesatierra.dtos.permission.PermissionRequestDto;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDto{

    @NotBlank
    private String name;

    @NotBlank
    private String color;

    @NotNull
    private Set<PermissionRequestDto> permissions;

    @NotBlank
    private String description;

    @Override
    public String toString() {
        return "RoleRequestDto{" +
                "color='" + color + '\'' +
                ", name='" + name + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}
