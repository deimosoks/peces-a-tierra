package org.icc.pecesatierra.features.role.repository;

import org.icc.pecesatierra.features.role.UserRole;
import org.icc.pecesatierra.features.role.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    long countByRoleId(String roleId);
    boolean existsByRoleId(String roleId);
}
