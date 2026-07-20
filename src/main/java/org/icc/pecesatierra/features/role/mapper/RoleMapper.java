package org.icc.pecesatierra.features.role.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.role.dtos.PermissionResponseDto;
import org.icc.pecesatierra.features.role.dtos.RoleRequestDto;
import org.icc.pecesatierra.features.role.dtos.RoleResponseDto;
import org.icc.pecesatierra.features.role.Role;
import org.icc.pecesatierra.features.role.RolePermission;
import org.icc.pecesatierra.features.role.RolePermissionId;
import org.icc.pecesatierra.features.role.repository.UserRoleRepository;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final UserRoleRepository userRoleRepository;
    private final DateTimeUtils dateTimeUtils;

    public RoleResponseDto toDto(Role role) {
        RoleResponseDto roleResponseDto = RoleResponseDto.builder()
                .id(role.getId())
                .createdAt(dateTimeUtils.toColombia(role.getCreatedAt()))
                .updatedAt(dateTimeUtils.toColombia(role.getUpdatedAt()))
                .name(role.getName())
                .color(role.getColor())
                .description(role.getDescription())
                .permissions(new HashSet<>())
                .totalUsers(userRoleRepository.countByRoleId(role.getId()))
                .build();
        roleResponseDto.getPermissions().addAll(
                role.getPermissions().stream().map(r -> new PermissionResponseDto(r.getId().getPermission())).collect(Collectors.toSet()));

        return roleResponseDto;
    }

    public void updateEntityFromDto(RoleRequestDto roleRequestDto, Role role) {

        role.setName(roleRequestDto.getName());
        role.setColor(roleRequestDto.getColor());
        role.setDescription(roleRequestDto.getDescription());

        role.getPermissions().clear();

        role.getPermissions().addAll(roleRequestDto.getPermissions().stream().map(p -> {
            RolePermissionId rolePermissionId = RolePermissionId.builder()
                    .roleId(role.getId())
                    .permission(p.getName().getPermission())
                    .build();

            return RolePermission.builder()
                    .id(rolePermissionId)
                    .role(role)
                    .build();
        }).collect(Collectors.toSet()));

    }
}
