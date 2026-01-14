package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.domain.entities.Permission;
import org.icc.pecesatierra.domain.entities.Role;
import org.icc.pecesatierra.domain.entities.RolePermission;
import org.icc.pecesatierra.domain.entities.RolePermissionId;
import org.icc.pecesatierra.exceptions.PermissionNotFoundException;
import org.icc.pecesatierra.exceptions.RoleNotFoundException;
import org.icc.pecesatierra.utils.mappers.RoleMapper;
import org.icc.pecesatierra.repositories.PermissionRepository;
import org.icc.pecesatierra.repositories.RoleRepository;
import org.icc.pecesatierra.repositories.UserRoleRepository;
import org.icc.pecesatierra.web.services.RoleService;
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

    @Transactional
    @Override
    public RoleResponseDto create(RoleRequestDto roleRequestDto) {

        Role role = roleRepository.save(Role.builder()
                .name(roleRequestDto.getName())
                .color(roleRequestDto.getColor())
                .createdAt(LocalDateTime.now())
                .description(roleRequestDto.getDescription())
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
                }
        );

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
    }

    @Override
    public void delete(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role doesn't exist"));
        roleRepository.delete(role);
    }

    @Override
    public List<RoleResponseDto> findAll() {
        return roleRepository.findAll().stream().map(roleMapper::toDto).toList();
    }
}
