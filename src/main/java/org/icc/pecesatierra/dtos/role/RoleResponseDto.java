package org.icc.pecesatierra.dtos.role;

import lombok.*;
import org.icc.pecesatierra.dtos.permission.PermissionRequestDto;
import org.icc.pecesatierra.dtos.permission.PermissionResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDto {

    private String id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<PermissionResponseDto> permissions;

}
