package org.icc.pecesatierra.features.role.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.role.Role;
import org.icc.pecesatierra.features.role.RolePermission;
import org.icc.pecesatierra.features.role.RolePermissionId;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.role.dtos.RoleRequestDto;
import org.icc.pecesatierra.features.role.dtos.RoleResponseDto;
import org.icc.pecesatierra.features.role.exceptions.CannotDeleteRoleInUseException;
import org.icc.pecesatierra.features.role.exceptions.RoleNotFoundException;
import org.icc.pecesatierra.features.role.repository.UserRoleRepository;
import org.icc.pecesatierra.features.role.mapper.RoleMapper;
import org.icc.pecesatierra.features.role.repository.RoleRepository;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserRoleRepository userRoleRepository;
    private final DateTimeUtils dateTimeUtils;

    @Transactional
    public RoleResponseDto create(RoleRequestDto roleRequestDto, User user) {

        Role role = roleRepository.save(Role.builder()
                .name(roleRequestDto.getName())
                .color(roleRequestDto.getColor())
                .createdAt(dateTimeUtils.nowUTC())
                .description(roleRequestDto.getDescription())
                .permissions(new HashSet<>())
                .build());

        roleRequestDto.getPermissions().forEach(
                permissionRequestDto -> {
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
        role.setUpdatedAt(dateTimeUtils.nowUTC());
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
    public List<RoleResponseDto> findAll() {
        return roleRepository.findAll().stream().map(roleMapper::toDto).toList();
    }
}
