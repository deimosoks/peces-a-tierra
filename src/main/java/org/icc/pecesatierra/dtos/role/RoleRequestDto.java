package org.icc.pecesatierra.dtos.role;

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

    @NotNull
    private String color;

    @NotNull
    private String name;

    @NotNull
    private Set<PermissionRequestDto> permissions;

    @Override
    public String toString() {
        return "RoleRequestDto{" +
                "color='" + color + '\'' +
                ", name='" + name + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}
