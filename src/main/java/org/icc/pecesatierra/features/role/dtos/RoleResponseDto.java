package org.icc.pecesatierra.features.role.dtos;

import lombok.*;

import java.time.LocalDateTime;
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
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String givenBy;
    private long totalUsers;
    private Set<PermissionResponseDto> permissions;

}