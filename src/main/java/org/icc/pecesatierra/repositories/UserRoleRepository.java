package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.UserRole;
import org.icc.pecesatierra.entities.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    long countByRoleId(String roleId);
    boolean existsByRoleId(String roleId);
}
