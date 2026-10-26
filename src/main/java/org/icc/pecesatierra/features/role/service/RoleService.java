package org.icc.pecesatierra.features.role.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.role.Role;
import org.icc.pecesatierra.features.role.RolePermission;
import org.icc.pecesatierra.features.role.RolePermissionId;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.role.dtos.RoleRequestDto;
import org.icc.pecesatierra.features.role.dtos.RoleResponseDto;
import org.icc.pecesatierra.features.role.dtos.PermissionRequestDto;
import org.icc.pecesatierra.features.role.exceptions.CannotDeleteRoleInUseException;
import org.icc.pecesatierra.features.role.exceptions.CannotGrantPermissionsYouDoNotHaveException;
import org.icc.pecesatierra.features.role.exceptions.CannotManageRoleWithMorePrivilegesException;
import org.icc.pecesatierra.features.role.exceptions.RoleNotFoundException;
import org.icc.pecesatierra.features.role.repository.UserRoleRepository;
import org.icc.pecesatierra.features.role.mapper.RoleMapper;
import org.icc.pecesatierra.features.role.repository.RoleRepository;
import org.icc.pecesatierra.features.role.repository.RoleUsersCount;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserRoleRepository userRoleRepository;
    private final DateTimeUtils dateTimeUtils;

    /**
     * Un ADMINISTRATOR conserva el bypass total sobre la gestión de roles.
     */
    private boolean isAdministrator(User user) {
        return user.hasAuthority(AppPermission.ADMINISTRATOR.name());
    }

    private Set<String> authoritiesOf(User user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Set<String> permissionNamesOf(Set<PermissionRequestDto> permissions) {
        return permissions.stream()
                .map(permission -> permission.getName().name())
                .collect(Collectors.toSet());
    }

    /**
     * Principio de subconjunto: un no-admin solo puede conceder permisos que ya tiene.
     */
    private void validateCanGrantPermissions(User user, Set<PermissionRequestDto> permissions) {
        if (isAdministrator(user)) {
            return;
        }

        if (!authoritiesOf(user).containsAll(permissionNamesOf(permissions))) {
            throw new CannotGrantPermissionsYouDoNotHaveException();
        }
    }

    /**
     * Un no-admin no puede editar ni borrar un rol con permisos que él no tenga.
     */
    private void validateCanManageRole(User user, Role role) {
        if (isAdministrator(user)) {
            return;
        }

        Set<String> rolePermissions = role.getPermissions().stream()
                .map(rolePermission -> rolePermission.getId().getPermission())
                .collect(Collectors.toSet());

        if (!authoritiesOf(user).containsAll(rolePermissions)) {
            throw new CannotManageRoleWithMorePrivilegesException();
        }
    }

    @Transactional
    public RoleResponseDto create(RoleRequestDto roleRequestDto, User user) {

        validateCanGrantPermissions(user, roleRequestDto.getPermissions());

        Role role = roleRepository.save(Role.builder()
                .name(roleRequestDto.getName())
                .color(roleRequestDto.getColor())
                .createdAt(dateTimeUtils.nowUTC())
                .description(roleRequestDto.getDescription())
                .permissions(new HashSet<>())
                .build());

        // El permiso es la clave natural: se deduplica antes de construir la
        // entidad para no intentar insertar dos veces la misma PK (H-45).
        roleRequestDto.getPermissions().stream()
                .map(PermissionRequestDto::getName)
                .distinct()
                .forEach(permission -> {
                    RolePermissionId rolePermissionId = RolePermissionId.builder()
                            .permission(permission.name())
                            .roleId(role.getId())
                            .build();

                    RolePermission rolePermission = RolePermission.builder()
                            .id(rolePermissionId)
                            .role(role)
                            .build();
                    role.getPermissions().add(rolePermission);
                });

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

        validateCanManageRole(user, role);

        validateCanGrantPermissions(user, roleRequestDto.getPermissions());

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

        validateCanManageRole(user, role);

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
        List<Role> roles = roleRepository.findAll();
        Map<String, Long> usersByRole = countUsersByRole(roles);

        return roles.stream()
                .map(role -> roleMapper.toDto(role, usersByRole.getOrDefault(role.getId(), 0L)))
                .toList();
    }

    /**
     * H-39: un solo conteo agregado para toda la lista. Antes el mapper lanzaba
     * {@code countByRoleId} por cada rol.
     */
    private Map<String, Long> countUsersByRole(List<Role> roles) {
        if (roles.isEmpty()) {
            return Map.of();
        }

        return userRoleRepository.countUsersByRoleIds(roles.stream().map(Role::getId).toList())
                .stream()
                .collect(Collectors.toMap(RoleUsersCount::getRoleId, RoleUsersCount::getTotal));
    }
}
