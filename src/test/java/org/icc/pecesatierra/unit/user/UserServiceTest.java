package org.icc.pecesatierra.unit.user;

import org.icc.pecesatierra.features.auth.RefreshToken;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.role.Role;
import org.icc.pecesatierra.features.role.RolePermission;
import org.icc.pecesatierra.features.role.RolePermissionId;
import org.icc.pecesatierra.features.role.repository.RoleRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.user.dtos.UserRequestDto;
import org.icc.pecesatierra.features.user.dtos.UserResponseDto;
import org.icc.pecesatierra.features.user.exceptions.CannotManageUserWithMorePrivilegesException;
import org.icc.pecesatierra.features.user.exceptions.CannotCreateUsersWithMembersOutsideYourBranch;
import org.icc.pecesatierra.features.user.exceptions.CannotUpdateUserWithMemberOutSideYourBranchException;
import org.icc.pecesatierra.features.user.mapper.UserMapper;
import org.icc.pecesatierra.features.user.repository.UserRepository;
import org.icc.pecesatierra.features.user.service.UserService;
import org.icc.pecesatierra.features.role.exceptions.CannotGrantPermissionsYouDoNotHaveException;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.specs.UserSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserSpecification userSpecification;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private UserService userService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");
    private final Branch branchB = TestFixtures.branch("branch-b", "Sede B");
    private final Member memberA = TestFixtures.member("member-a", branchA, "Ana A");
    private final Member memberA2 = TestFixtures.member("member-a2", branchA, "Ana A Dos");
    private final Member memberB = TestFixtures.member("member-b", branchB, "Beto B");

    private User operator(String... permissions) {
        return TestFixtures.user("user-op", "operador", TestFixtures.member("op-member", branchA, "Operador"), permissions);
    }

    private User administrator() {
        return TestFixtures.user("user-admin", "admin", TestFixtures.member("admin-member", branchA, "Admin"),
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

    private UserRequestDto request(String memberId, String username, String password, Set<String> rolesId) {
        return UserRequestDto.builder()
                .memberId(memberId)
                .username(username)
                .password(password)
                .rolesId(rolesId)
                .build();
    }

    // ---------- H-04: asignacion de roles al crear usuarios ----------

    @Test
    void nonAdminCannotCreateUserWithAdministratorRole_H04() {
        User actor = operator(AppPermission.CREATE_USER.name(), AppPermission.UPDATE_USER.name());
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);

        when(userRepository.existsByMemberId(memberA2.getId())).thenReturn(false);
        when(userRepository.findByUsername("nuevo-usuario")).thenReturn(Optional.empty());
        when(memberRepository.findById(memberA2.getId())).thenReturn(Optional.of(memberA2));
        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));

        UserRequestDto request = request(memberA2.getId(), "nuevo-usuario", "password123", Set.of("role-admin"));

        assertThatThrownBy(() -> userService.create(request, actor))
                .isInstanceOfSatisfying(CannotGrantPermissionsYouDoNotHaveException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(userMapper);
    }

    @Test
    void nonAdminCanCreateUserGrantingPermissionsItAlreadyHas_H04() {
        User actor = operator(AppPermission.CREATE_USER.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        Role allowedRole = role("role-view", AppPermission.VIEW_MEMBER_PANEL);

        when(userRepository.existsByMemberId(memberA2.getId())).thenReturn(false);
        when(userRepository.findByUsername("nuevo-usuario")).thenReturn(Optional.empty());
        when(memberRepository.findById(memberA2.getId())).thenReturn(Optional.of(memberA2));
        when(roleRepository.findById("role-view")).thenReturn(Optional.of(allowedRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(userMapper.toDto(any(User.class))).thenReturn(UserResponseDto.builder().id("user-new").build());

        UserResponseDto response = userService.create(
                request(memberA2.getId(), "nuevo-usuario", "password123", Set.of("role-view")), actor);

        assertThat(response.getId()).isEqualTo("user-new");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRoles()).hasSize(1);
        assertThat(saved.hasAuthority(AppPermission.VIEW_MEMBER_PANEL.name())).isTrue();
        assertThat(saved.hasAuthority(AppPermission.ADMINISTRATOR.name())).isFalse();
    }

    // ---------- H-21: cuentas con mas privilegios ----------

    @Test
    void nonAdminCannotUpdateAdministrator_H21() {
        User actor = operator(AppPermission.UPDATE_USER.name(), AppPermission.VIEW_USER_PANEL.name());
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberA,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.findById(memberA.getId())).thenReturn(Optional.of(memberA));

        UserRequestDto request = request(memberA.getId(), "admin-dos", null, Set.of("role-view"));

        assertThatThrownBy(() -> userService.update(request, target.getId(), actor))
                .isInstanceOfSatisfying(CannotManageUserWithMorePrivilegesException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(passwordEncoder);
        assertThat(target.getUsername()).isEqualTo("admin-dos");
        assertThat(target.getRoles()).hasSize(1);
        assertThat(target.hasAuthority(AppPermission.ADMINISTRATOR.name())).isTrue();
    }

    @Test
    void nonAdminCannotDeactivateAdministrator_H21() {
        User actor = operator(AppPermission.UPDATE_USER.name());
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberA,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.updateActive(actor, target.getId(), false))
                .isInstanceOfSatisfying(CannotManageUserWithMorePrivilegesException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any(User.class));
        assertThat(target.isActive()).isTrue();
    }

    @Test
    void nonAdminCannotDeleteAdministrator_H21() {
        User actor = operator(AppPermission.DELETE_USER.name());
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberA,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.delete(actor, target.getId()))
                .isInstanceOfSatisfying(CannotManageUserWithMorePrivilegesException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).delete(any(User.class));
    }

    // ---------- H-05: sede del nuevo integrante y refresh tokens ----------

    @Test
    void nonAdminCannotMoveUserToMemberOfAnotherBranch_H05() {
        User actor = operator(AppPermission.UPDATE_USER.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        User target = TestFixtures.user("user-target", "usuario-destino", memberA,
                AppPermission.VIEW_MEMBER_PANEL.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.findById(memberB.getId())).thenReturn(Optional.of(memberB));
        when(userRepository.existsByMemberId(memberB.getId())).thenReturn(false);

        UserRequestDto request = request(memberB.getId(), "usuario-destino", null, Set.of("role-view"));

        assertThatThrownBy(() -> userService.update(request, target.getId(), actor))
                .isInstanceOfSatisfying(CannotUpdateUserWithMemberOutSideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository);
        assertThat(target.getMember()).isSameAs(memberA);
    }

    @Test
    void passwordChangeRevokesRefreshTokens_H05() {
        User actor = operator(AppPermission.UPDATE_USER.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        User target = TestFixtures.user("user-target", "usuario-destino", memberA,
                AppPermission.VIEW_MEMBER_PANEL.name());
        target.getRefreshTokens().add(RefreshToken.builder()
                .id("refresh-1")
                .token("token-1")
                .createdAt(new Date())
                .expiresAt(new Date())
                .user(target)
                .build());
        Role allowedRole = role("role-view", AppPermission.VIEW_MEMBER_PANEL);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.findById(memberA.getId())).thenReturn(Optional.of(memberA));
        when(roleRepository.findById("role-view")).thenReturn(Optional.of(allowedRole));
        when(passwordEncoder.matches("nueva-password", "not-a-real-password-hash")).thenReturn(false);
        when(passwordEncoder.encode("nueva-password")).thenReturn("new-hash");
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(userMapper.toDto(any(User.class))).thenReturn(UserResponseDto.builder().id(target.getId()).build());

        userService.update(
                request(memberA.getId(), "usuario-destino", "nueva-password", Set.of("role-view")),
                target.getId(), actor);

        assertThat(target.getRefreshTokens()).isEmpty();
        assertThat(target.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(target);
    }

    @Test
    void updateWithoutPasswordChangeKeepsRefreshTokens_H05() {
        User actor = operator(AppPermission.UPDATE_USER.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        User target = TestFixtures.user("user-target", "usuario-destino", memberA,
                AppPermission.VIEW_MEMBER_PANEL.name());
        target.getRefreshTokens().add(RefreshToken.builder()
                .id("refresh-1")
                .token("token-1")
                .createdAt(new Date())
                .expiresAt(new Date())
                .user(target)
                .build());
        Role allowedRole = role("role-view", AppPermission.VIEW_MEMBER_PANEL);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.findById(memberA.getId())).thenReturn(Optional.of(memberA));
        when(roleRepository.findById("role-view")).thenReturn(Optional.of(allowedRole));
        when(passwordEncoder.matches("misma-password", "not-a-real-password-hash")).thenReturn(true);
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(userMapper.toDto(any(User.class))).thenReturn(UserResponseDto.builder().id(target.getId()).build());

        userService.update(
                request(memberA.getId(), "usuario-destino", "misma-password", Set.of("role-view")),
                target.getId(), actor);

        assertThat(target.getRefreshTokens()).hasSize(1);
        assertThat(target.getPasswordHash()).isEqualTo("not-a-real-password-hash");
    }

    @Test
    void nonAdminCannotLearnPrivilegesOfAnAccountInAnotherBranch_M16() {
        User actor = operator(AppPermission.UPDATE_USER.name());
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberB,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        // La sede se comprueba antes que los privilegios: 409, no 403.
        assertThatThrownBy(() -> userService.updateActive(actor, target.getId(), false))
                .isInstanceOfSatisfying(CannotUpdateUserWithMemberOutSideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any(User.class));
        assertThat(target.isActive()).isTrue();
    }

    @Test
    void nonAdminCannotLearnPrivilegesOfAnAccountInAnotherBranchOnDelete_M16() {
        User actor = operator(AppPermission.DELETE_USER.name());
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberB,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.delete(actor, target.getId()))
                .isInstanceOfSatisfying(CannotUpdateUserWithMemberOutSideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).delete(any(User.class));
    }

    // ---------- M-23: oraculo de cuentas de integrantes ajenos ----------

    @Test
    void createForMemberOfAnotherBranchDoesNotRevealWhetherItHasAnAccount_M23() {
        User actor = operator(AppPermission.CREATE_USER.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        when(memberRepository.findById(memberB.getId())).thenReturn(Optional.of(memberB));

        assertThatThrownBy(() -> userService.create(
                request(memberB.getId(), "nuevo-usuario", "password123", Set.of("role-view")), actor))
                .isInstanceOfSatisfying(CannotCreateUsersWithMembersOutsideYourBranch.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        // Ni la unicidad del integrante ni la del username se consultan antes de la sede.
        verify(userRepository, never()).existsByMemberId(any());
        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- M-24: memberId nulo al actualizar ----------

    @Test
    void updateWithoutMemberIdDoesNotValidateMemberBranch_M24() {
        User actor = operator(AppPermission.UPDATE_USER.name(), AppPermission.VIEW_MEMBER_PANEL.name());
        User target = TestFixtures.user("user-target", "usuario-destino", memberA,
                AppPermission.VIEW_MEMBER_PANEL.name());
        Role allowedRole = role("role-view", AppPermission.VIEW_MEMBER_PANEL);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(roleRepository.findById("role-view")).thenReturn(Optional.of(allowedRole));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(userMapper.toDto(any(User.class))).thenReturn(UserResponseDto.builder().id(target.getId()).build());

        userService.update(
                request(null, "usuario-destino-actualizado", null, Set.of("role-view")),
                target.getId(), actor);

        assertThat(target.getMember()).isSameAs(memberA);
        assertThat(target.getUsername()).isEqualTo("usuario-destino-actualizado");
        verify(userRepository).save(target);
    }

    // ---------- El administrador conserva el bypass ----------

    @Test
    void administratorCanCreateUserWithAdministratorRole_H04() {
        User actor = administrator();
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);

        when(userRepository.existsByMemberId(memberA2.getId())).thenReturn(false);
        when(userRepository.findByUsername("nuevo-admin")).thenReturn(Optional.empty());
        when(memberRepository.findById(memberA2.getId())).thenReturn(Optional.of(memberA2));
        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(userMapper.toDto(any(User.class))).thenReturn(UserResponseDto.builder().id("user-new").build());

        UserResponseDto response = userService.create(
                request(memberA2.getId(), "nuevo-admin", "password123", Set.of("role-admin")), actor);

        assertThat(response.getId()).isEqualTo("user-new");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().hasAuthority(AppPermission.ADMINISTRATOR.name())).isTrue();
    }

    @Test
    void administratorCanUpdateAnotherAdministrator_H21() {
        User actor = administrator();
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberA,
                AppPermission.ADMINISTRATOR.name());
        Role administratorRole = role("role-admin", AppPermission.ADMINISTRATOR);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.findById(memberA.getId())).thenReturn(Optional.of(memberA));
        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(administratorRole));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(userMapper.toDto(any(User.class))).thenReturn(UserResponseDto.builder().id(target.getId()).build());

        userService.update(
                request(memberA.getId(), "admin-dos-actualizado", null, Set.of("role-admin")),
                target.getId(), actor);

        assertThat(target.getUsername()).isEqualTo("admin-dos-actualizado");
        assertThat(target.hasAuthority(AppPermission.ADMINISTRATOR.name())).isTrue();
        verify(userRepository).save(target);
    }

    @Test
    void administratorCanDeactivateAnotherAdministrator_H21() {
        User actor = administrator();
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberA,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);

        boolean active = userService.updateActive(actor, target.getId(), false);

        assertThat(active).isFalse();
        verify(userRepository).save(target);
    }

    @Test
    void administratorCanDeleteAnotherAdministrator_H21() {
        User actor = administrator();
        User target = TestFixtures.user("user-admin-target", "admin-dos", memberA,
                AppPermission.ADMINISTRATOR.name());

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        userService.delete(actor, target.getId());

        verify(userRepository).delete(target);
    }
}
