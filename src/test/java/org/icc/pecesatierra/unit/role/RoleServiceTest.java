package org.icc.pecesatierra.unit.role;

import org.icc.pecesatierra.features.role.Role;
import org.icc.pecesatierra.features.role.RolePermission;
import org.icc.pecesatierra.features.role.RolePermissionId;
import org.icc.pecesatierra.features.role.dtos.PermissionRequestDto;
import org.icc.pecesatierra.features.role.dtos.RoleRequestDto;
import org.icc.pecesatierra.features.role.dtos.RoleResponseDto;
import org.icc.pecesatierra.features.role.exceptions.CannotGrantPermissionsYouDoNotHaveException;
import org.icc.pecesatierra.features.role.exceptions.CannotManageRoleWithMorePrivilegesException;
import org.icc.pecesatierra.features.role.mapper.RoleMapper;
import org.icc.pecesatierra.features.role.repository.RoleRepository;
import org.icc.pecesatierra.features.role.repository.UserRoleRepository;
import org.icc.pecesatierra.features.role.service.RoleService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private RoleService roleService;

    private User operator(String... permissions) {
        return TestFixtures.user("user-op", "operador",
                TestFixtures.member("op-member", TestFixtures.branch("branch-a", "Sede A"), "Operador"),
                permissions);
    }

    private User administrator() {
        return TestFixtures.user("user-admin", "admin",
                TestFixtures.member("admin-member", TestFixtures.branch("branch-a", "Sede A"), "Admin"),
                AppPermission.ADMINISTRATOR.name());
    }

    private Role role(String id, AppPermission... permissions) {
        Role role = Role.builder()
                .id(id)
                .name("Rol " + id)
                .color("#123456")
                .description("Rol de prueba")
                .createdAt(TestFixtures.FIXED_NOW)
                .permissions(new HashSet<>())
                .build();

        for (AppPermission permission : permissions) {
            role.getPermissions().add(RolePermission.builder()
                    .id(new RolePermissionId(permission.name(), id))
                    .role(role)
                    .build());
        }

        return role;
    }

    private RoleRequestDto roleRequest(String name, AppPermission... permissions) {
        Set<PermissionRequestDto> dtos = Arrays.stream(permissions)
                .map(permission -> PermissionRequestDto.builder().name(permission).build())
                .collect(Collectors.toSet());

        return RoleRequestDto.builder()
                .name(name)
                .color("#123456")
                .description("Rol de prueba")
                .permissions(dtos)
                .build();
    }

    // ---------- H-04: creacion de roles ----------

    @Test
    void nonAdminCannotCreateRoleWithAdministratorPermission_H04() {
        User actor = operator(AppPermission.CREATE_ROLE.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        RoleRequestDto request = roleRequest("Administrador", AppPermission.ADMINISTRATOR);

        assertThatThrownBy(() -> roleService.create(request, actor))
                .isInstanceOfSatisfying(CannotGrantPermissionsYouDoNotHaveException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(roleRepository, never()).save(any(Role.class));
        verifyNoInteractions(roleMapper);
        verifyNoInteractions(dateTimeUtils);
    }

    @Test
    void nonAdminCanCreateRoleWithPermissionsItAlreadyHas_H04() {
        User actor = operator(AppPermission.CREATE_ROLE.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        RoleRequestDto request = roleRequest("Operativo", AppPermission.VIEW_MEMBER_PANEL);

        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId("role-new");
            return role;
        });
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(roleMapper.toDto(any(Role.class))).thenReturn(RoleResponseDto.builder().id("role-new").build());

        RoleResponseDto response = roleService.create(request, actor);

        assertThat(response.getId()).isEqualTo("role-new");

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions().stream()
                .map(permission -> permission.getId().getPermission())
                .toList())
                .containsExactly(AppPermission.VIEW_MEMBER_PANEL.name());
    }

    // ---------- H-04: edicion y borrado de roles ----------

    @Test
    void nonAdminCannotAddAdministratorPermissionToExistingRole_H04() {
        User actor = operator(AppPermission.UPDATE_ROLE.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        Role existing = role("role-1", AppPermission.VIEW_MEMBER_PANEL);
        RoleRequestDto request = roleRequest("Operativo",
                AppPermission.VIEW_MEMBER_PANEL, AppPermission.ADMINISTRATOR);

        when(roleRepository.findById("role-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> roleService.update(request, "role-1", actor))
                .isInstanceOfSatisfying(CannotGrantPermissionsYouDoNotHaveException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(roleMapper, never()).updateEntityFromDto(any(RoleRequestDto.class), any(Role.class));
        verify(roleRepository, never()).save(any(Role.class));
        assertThat(existing.getPermissions()).hasSize(1);
        assertThat(existing.getPermissions().iterator().next().getId().getPermission())
                .isEqualTo(AppPermission.VIEW_MEMBER_PANEL.name());
    }

    @Test
    void nonAdminCannotUpdateRoleWithMorePrivileges_H04() {
        User actor = operator(AppPermission.UPDATE_ROLE.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);
        RoleRequestDto request = roleRequest("Administrador", AppPermission.VIEW_MEMBER_PANEL);

        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));

        assertThatThrownBy(() -> roleService.update(request, "role-admin", actor))
                .isInstanceOfSatisfying(CannotManageRoleWithMorePrivilegesException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(roleMapper, never()).updateEntityFromDto(any(RoleRequestDto.class), any(Role.class));
        verify(roleRepository, never()).save(any(Role.class));
        assertThat(administratorRole.getPermissions()).hasSize(1);
    }

    @Test
    void nonAdminCannotDeleteRoleWithMorePrivileges_H04() {
        User actor = operator(AppPermission.DELETE_ROLE.name());
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);

        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));

        assertThatThrownBy(() -> roleService.delete("role-admin", actor))
                .isInstanceOfSatisfying(CannotManageRoleWithMorePrivilegesException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(roleRepository, never()).delete(any(Role.class));
        verifyNoInteractions(userRoleRepository);
    }

    // ---------- H-45: permisos repetidos en el payload ----------

    @Test
    void createWithRepeatedPermissionPersistsOnlyOne_H45() {
        User actor = administrator();
        // PermissionRequestDto no tiene equals/hashCode: dos instancias con el
        // mismo enum llegan como dos elementos del Set, tal como desde la API.
        RoleRequestDto request = roleRequest("Duplicado",
                AppPermission.VIEW_MEMBER_PANEL, AppPermission.VIEW_MEMBER_PANEL);
        assertThat(request.getPermissions()).hasSize(2);

        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId("role-dup");
            return role;
        });
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(roleMapper.toDto(any(Role.class))).thenReturn(RoleResponseDto.builder().id("role-dup").build());

        roleService.create(request, actor);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions()).hasSize(1);
        assertThat(captor.getValue().getPermissions())
                .extracting(permission -> permission.getId().getPermission())
                .containsExactly(AppPermission.VIEW_MEMBER_PANEL.name());
    }

    @Test
    void updateWithRepeatedPermissionDoesNotFailAndKeepsOnlyOne_H45() {
        User actor = administrator();
        Role existing = role("role-1", AppPermission.VIEW_MEMBER_PANEL);
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(existing));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        // Mapper real: la deduplicacion del PUT vive en updateEntityFromDto.
        RoleService serviceWithRealMapper = new RoleService(
                roleRepository, new RoleMapper(userRoleRepository, dateTimeUtils),
                userRoleRepository, dateTimeUtils);

        RoleRequestDto request = roleRequest("Rol role-1",
                AppPermission.VIEW_MEMBER_PANEL, AppPermission.VIEW_MEMBER_PANEL);
        assertThat(request.getPermissions()).hasSize(2);

        RoleResponseDto response = serviceWithRealMapper.update(request, "role-1", actor);

        assertThat(response.getId()).isEqualTo("role-1");
        assertThat(existing.getPermissions()).hasSize(1);
        assertThat(existing.getPermissions())
                .extracting(permission -> permission.getId().getPermission())
                .containsExactly(AppPermission.VIEW_MEMBER_PANEL.name());
        verify(roleRepository).save(existing);
    }

    // ---------- El administrador conserva el bypass ----------

    @Test
    void administratorCanCreateRoleWithAdministratorPermission_H04() {
        User actor = administrator();
        RoleRequestDto request = roleRequest("Administrador", AppPermission.ADMINISTRATOR);

        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId("role-admin-new");
            return role;
        });
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(roleMapper.toDto(any(Role.class))).thenReturn(RoleResponseDto.builder().id("role-admin-new").build());

        RoleResponseDto response = roleService.create(request, actor);

        assertThat(response.getId()).isEqualTo("role-admin-new");

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions().stream()
                .map(permission -> permission.getId().getPermission())
                .toList())
                .containsExactly(AppPermission.ADMINISTRATOR.name());
    }

    @Test
    void administratorCanUpdateRoleWithMorePrivileges_H04() {
        User actor = administrator();
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);
        RoleRequestDto request = roleRequest("Administrador", AppPermission.ADMINISTRATOR);

        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(roleMapper.toDto(administratorRole))
                .thenReturn(RoleResponseDto.builder().id("role-admin").build());

        RoleResponseDto response = roleService.update(request, "role-admin", actor);

        assertThat(response.getId()).isEqualTo("role-admin");
        verify(roleMapper).updateEntityFromDto(any(RoleRequestDto.class), eq(administratorRole));
        verify(roleRepository).save(administratorRole);
    }

    @Test
    void administratorCanDeleteRoleWithAdministratorPermission_H04() {
        User actor = administrator();
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);

        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));
        when(userRoleRepository.existsByRoleId("role-admin")).thenReturn(false);

        roleService.delete("role-admin", actor);

        verify(roleRepository).delete(administratorRole);
    }
}
