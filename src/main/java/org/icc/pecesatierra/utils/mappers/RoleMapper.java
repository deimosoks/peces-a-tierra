package org.icc.pecesatierra.utils.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.permission.PermissionRequestDto;
import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.domain.entities.Permission;
import org.icc.pecesatierra.domain.entities.Role;
import org.icc.pecesatierra.domain.entities.RolePermission;
import org.icc.pecesatierra.domain.entities.RolePermissionId;
import org.icc.pecesatierra.exceptions.PermissionNotFoundException;
import org.icc.pecesatierra.repositories.PermissionRepository;
import org.icc.pecesatierra.repositories.UserRoleRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

//@Mapper(componentModel = "spring")
@Component
@AllArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleResponseDto toDto(Role role){
        RoleResponseDto roleResponseDto = RoleResponseDto.builder()
                .id(role.getId())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .name(role.getName())
                .color(role.getColor())
                .description(role.getDescription())
                .permissions(new HashSet<>())
                .totalUsers(userRoleRepository.countByRoleId(role.getId()))
                .build();
                roleResponseDto.getPermissions().addAll(
                    role.getPermissions().stream().map(r ->
                            permissionMapper.toDto(
                                r.getPermission())
                    ).collect(Collectors.toSet())
        );

        return roleResponseDto;
    }

//    @Mapping(target = "permissions", ignore = true)
    public void updateEntityFromDto(RoleRequestDto roleRequestDto, Role role){

        role.setName(roleRequestDto.getName());
        role.setColor(roleRequestDto.getColor());
        role.setDescription(roleRequestDto.getDescription());

        Set<String> requestedPermissions = roleRequestDto.getPermissions().stream()
                .map(PermissionRequestDto::getName)
                .collect(Collectors.toSet());

        Set<String> currentPermissions = role.getPermissions().stream()
                .map(rolePermission -> rolePermission.getPermission().getName())
                .collect(Collectors.toSet());

        role.getPermissions().removeIf(
                rolePermission -> !requestedPermissions.contains(rolePermission.getPermission().getName())
        );

        requestedPermissions.forEach(permissionName -> {
            if (!currentPermissions.contains(permissionName)){
                Permission permission = permissionRepository.findById(permissionName)
                        .orElseThrow(() -> new PermissionNotFoundException("Permission doesn't exist."));

                RolePermission rolePermission = RolePermission.builder()
                        .id(RolePermissionId.builder()
                                .roleId(role.getId())
                                .permissionId(permissionName)
                                .build()
                        )
                        .role(role)
                        .permission(permission)
                        .build();

                role.getPermissions().add(rolePermission);
            }
        });
    }
}
