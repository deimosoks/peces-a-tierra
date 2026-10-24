package org.icc.pecesatierra.support;

import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.role.Role;
import org.icc.pecesatierra.features.role.RolePermission;
import org.icc.pecesatierra.features.role.RolePermissionId;
import org.icc.pecesatierra.features.role.UserRole;
import org.icc.pecesatierra.features.role.UserRoleId;
import org.icc.pecesatierra.features.user.User;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Fixtures de entidades para pruebas unitarias. No arranca Spring ni toca la base
 * de datos: solo construye objetos con las relaciones que necesita cada servicio.
 */
public final class TestFixtures {

    public static final OffsetDateTime FIXED_NOW = OffsetDateTime.parse("2026-01-15T12:00:00Z");

    private TestFixtures() {
    }

    public static Branch branch(String id, String name) {
        return Branch.builder()
                .id(id)
                .name(name)
                .address("Calle 1 # 2-3")
                .city("Bogota")
                .cellphone("3000000000")
                .createdAt(FIXED_NOW)
                .build();
    }

    public static Member member(String id, Branch branch, String completeName) {
        return Member.builder()
                .id(id)
                .completeName(completeName)
                .branch(branch)
                .active(true)
                .createdAt(FIXED_NOW)
                .build();
    }

    public static User user(String id, String username, Member member, String... permissions) {
        Role role = Role.builder()
                .id("role-" + id)
                .name("ROLE_" + username)
                .color("#000000")
                .description("Rol de prueba")
                .createdAt(FIXED_NOW)
                .permissions(new HashSet<>())
                .build();

        for (String permission : permissions) {
            role.getPermissions().add(RolePermission.builder()
                    .id(new RolePermissionId(permission, role.getId()))
                    .role(role)
                    .build());
        }

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(role.getId(), id))
                .role(role)
                .givenDate(FIXED_NOW)
                .build();

        return User.builder()
                .id(id)
                .username(username)
                .passwordHash("not-a-real-password-hash")
                .active(true)
                .createdAt(FIXED_NOW)
                .member(member)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
    }
}
