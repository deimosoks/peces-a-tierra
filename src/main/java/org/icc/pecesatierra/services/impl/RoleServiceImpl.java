package org.icc.pecesatierra.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.entities.Permission;
import org.icc.pecesatierra.entities.Role;
import org.icc.pecesatierra.entities.RolePermission;
import org.icc.pecesatierra.entities.RolePermissionId;
import org.icc.pecesatierra.exceptions.PermissionNotFoundException;
import org.icc.pecesatierra.exceptions.RoleNotFoundException;
import org.icc.pecesatierra.mappers.RoleMapper;
import org.icc.pecesatierra.repositories.PermissionRepository;
import org.icc.pecesatierra.repositories.RoleRepository;
import org.icc.pecesatierra.repositories.UserRoleRepository;
import org.icc.pecesatierra.services.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    @Override
    public RoleResponseDto create(RoleRequestDto roleRequestDto) {

        Role role = roleRepository.save(Role.builder()
                .name(roleRequestDto.getName())
                .color(roleRequestDto.getColor())
                .createdAt(LocalDateTime.now())
                .permissions(new HashSet<>())
                .build());

        roleRequestDto.getPermissions().forEach(
                permissionRequestDto -> {
                    Permission permission = permissionRepository.findById(permissionRequestDto.getName())
                            .orElseThrow(() -> new PermissionNotFoundException("Permission doesn't exist."));

                    RolePermissionId rolePermissionId = RolePermissionId.builder()
                            .permissionId(permission.getName())
                            .roleId(role.getId())
                            .build();

                    RolePermission rolePermission = RolePermission.builder()
                            .id(rolePermissionId)
                            .permission(permission)
                            .role(role)
                            .build();
                    role.getPermissions().add(rolePermission);
//                    rolePermissionRepository.save(rolePermission);
                }
        );

//        RoleResponseDto roleResponseDto = roleMapper.toDto(role);
//        roleResponseDto.getPermissions().clear();
//        roleResponseDto.getPermissions().addAll(role.getPermissions().stream().map(
//                rolePermission -> new PermissionResponseDto(rolePermission.getPermission().getName())
//        ).collect(Collectors.toSet()));

        return roleMapper.toDto(role);
    }

    @Transactional
    @Override
    public RoleResponseDto update(RoleRequestDto roleRequestDto, String rolId) {

        Role role = roleRepository.findById(rolId)
                .orElseThrow(() -> new RoleNotFoundException("Role doesn't exist."));

        roleMapper.updateEntityFromDto(roleRequestDto, role);
        role.setUpdatedAt(LocalDateTime.now());

        return roleMapper.toDto(roleRepository.save(role));
//                RoleResponseDto.builder()
//                .id(role.getId())
//                .color(role.getColor())
//                .name(role.getName())
//                .createdAt(role.getCreatedAt())
//                .totalUsers(userRoleRepository.countByRoleId(role.getId()))
//                .permissions(role.getPermissions().stream().map(rolePermission ->
//                        PermissionResponseDto.builder()
//                                .name(rolePermission.getPermission().getName())
//                                .build()).collect(Collectors.toSet()))
//                .updatedAt(LocalDateTime.now())
//                .build();

//        Set<String> requestedPermissions = roleRequestDto.getPermissions().stream()
//                .map(PermissionRequestDto::getName)
//                .collect(Collectors.toSet());
//
//        Set<String> currentPermissions = role.getPermissions().stream()
//                .map(rolePermission -> rolePermission.getPermission().getName())
//                .collect(Collectors.toSet());
//
//        role.getPermissions().removeIf(
//                rolePermission -> !requestedPermissions.contains(rolePermission.getPermission().getName())
//                );
//
//        requestedPermissions.forEach(permissionName -> {
//            if (!currentPermissions.contains(permissionName)){
//                Permission permission = permissionRepository.findById(permissionName)
//                        .orElseThrow(() -> new PermissionNotFoundException("Permission doesn't exist."));
//
//                RolePermission rolePermission = RolePermission.builder()
//                        .id(RolePermissionId.builder()
//                                .roleId(role.getId())
//                                .permissionId(permissionName)
//                                .build()
//                        )
//                        .role(role)
//                        .permission(permission)
//                        .build();
//
//                role.getPermissions().add(rolePermission);
//            }
//        });

    }

    @Override
    public void delete(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role doesn't exist"));
        roleRepository.delete(role);
    }

    @Override
    public List<RoleResponseDto> findAll() {
        return roleRepository.findAll().stream().map(role -> {
//            RoleResponseDto roleResponseDto = roleMapper.toDto(role);
//            roleResponseDto.setTotalUsers(userRoleRepository.countByRoleId(role.getId()));
//            roleResponseDto.getPermissions().clear();
//            roleResponseDto.getPermissions().addAll(
//                    role.getPermissions().stream().map(rolePermission -> PermissionResponseDto.builder()
//                            .name(rolePermission.getPermission().getName())
//                            .build()).collect(Collectors.toSet())
//            );
            return roleMapper.toDto(role);
        }).toList();
    }
}
