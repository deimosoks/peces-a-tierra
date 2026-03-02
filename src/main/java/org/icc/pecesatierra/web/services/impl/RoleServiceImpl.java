package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.role.RoleRequestDto;
import org.icc.pecesatierra.dtos.role.RoleResponseDto;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.exceptions.roles.CannotDeleteRoleInUseException;
import org.icc.pecesatierra.exceptions.roles.RoleNotFoundException;
import org.icc.pecesatierra.repositories.UserRoleRepository;
import org.icc.pecesatierra.utils.mappers.RoleMapper;
import org.icc.pecesatierra.repositories.RoleRepository;
import org.icc.pecesatierra.web.services.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    @Override
    public RoleResponseDto create(RoleRequestDto roleRequestDto, User user) {

        Role role = roleRepository.save(Role.builder()
                .name(roleRequestDto.getName())
                .color(roleRequestDto.getColor())
                .createdAt(LocalDateTime.now())
                .description(roleRequestDto.getDescription())
                .permissions(new HashSet<>())
                .build());

        roleRequestDto.getPermissions().forEach(
                permissionRequestDto -> {
//                    Permission permission = permissionRepository.findById(permissionRequestDto.getPermission())
//                            .orElseThrow(PermissionNotFoundException::new);

                    RolePermissionId rolePermissionId = RolePermissionId.builder()
                            .permission(permissionRequestDto.getName().getPermission())
                            .roleId(role.getId())
                            .build();

                    RolePermission rolePermission = RolePermission.builder()
                            .id(rolePermissionId)
                            .role(role)
                            .build();
                    role.getPermissions().add(rolePermission);
                }
        );

        log.info("""
                        Usuario {} creó un rol:
                        ID: {}
                        Nombre: {}
                        Color: {}
                        Descripción: {}
                        Permisos: {}
                        """,
                user.getMember().getId(),
                role.getId(),
                role.getName(),
                role.getColor(),
                role.getDescription(),
                role.getPermissions().stream().map(p -> p.getId().getPermission()).toList()
        );

        return roleMapper.toDto(role);
    }

    @Transactional
    @Override
    public RoleResponseDto update(RoleRequestDto roleRequestDto, String rolId, User user) {

        Role role = roleRepository.findById(rolId)
                .orElseThrow(RoleNotFoundException::new);

        Role beforeUpdate = Role.builder()
                .id(role.getId())
                .name(role.getName())
                .color(role.getColor())
                .description(role.getDescription())
                .permissions(new HashSet<>(role.getPermissions()))
                .build();

        roleMapper.updateEntityFromDto(roleRequestDto, role);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        log.info("""
                        Usuario {} actualizó el rol {}.
                        Estado anterior:
                        Nombre: {}
                        Color: {}
                        Descripción: {}
                        Permisos: {}
                        Nuevo estado:
                        Nombre: {}
                        Color: {}
                        Descripción: {}
                        Permisos: {}
                        """,
                user.getMember().getId(),
                role.getId(),
                beforeUpdate.getName(),
                beforeUpdate.getColor(),
                beforeUpdate.getDescription(),
                beforeUpdate.getPermissions().stream().map(p -> p.getId().getPermission()).toList(),
                role.getName(),
                role.getColor(),
                role.getDescription(),
                role.getPermissions().stream().map(p -> p.getId().getPermission()).toList()
        );

        return roleMapper.toDto(role);
    }

    @Transactional
    @Override
    public void delete(String roleId, User user) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(RoleNotFoundException::new);

        if (userRoleRepository.existsByRoleId(roleId)) {
            log.warn("Usuario {} intento eliminar rol '{}' pero está en uso.", user.getMember().getId(), role.getName());
            throw new CannotDeleteRoleInUseException(role.getName());
        }

        log.info("""
                        Usuario {} eliminó el rol:
                        ID: {}
                        Nombre: {}
                        Color: {}
                        Descripción: {}
                        Permisos: {}
                        """,
                user.getMember().getId(),
                role.getId(),
                role.getName(),
                role.getColor(),
                role.getDescription(),
                role.getPermissions().stream().map(p -> p.getId().getPermission()).toList()
        );

        roleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoleResponseDto> findAll() {
        return roleRepository.findAll().stream().map(roleMapper::toDto).toList();
    }
}
