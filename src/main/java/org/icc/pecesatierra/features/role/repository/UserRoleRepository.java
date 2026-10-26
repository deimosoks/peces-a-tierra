package org.icc.pecesatierra.features.role.repository;

import org.icc.pecesatierra.features.role.UserRole;
import org.icc.pecesatierra.features.role.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    long countByRoleId(String roleId);
    boolean existsByRoleId(String roleId);

    /**
     * H-39: un unico conteo agregado para todos los roles de la lista. Antes
     * {@code RoleMapper.toDto} llamaba a {@link #countByRoleId(String)} por cada
     * rol y la consulta de roles hacia N+1.
     */
    @Query("""
            SELECT ur.role.id AS roleId, COUNT(ur) AS total
            FROM UserRole ur
            WHERE ur.role.id IN :roleIds
            GROUP BY ur.role.id
            """)
    List<RoleUsersCount> countUsersByRoleIds(@Param("roleIds") Collection<String> roleIds);
}
