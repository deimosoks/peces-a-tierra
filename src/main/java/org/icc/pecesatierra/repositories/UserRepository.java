package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.User;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {
            "member",
            "member.branch",
            "roles",
            "roles.role",
            "roles.role.permissions.id.permission"
    })
    Optional<User> findByUsername(String username);

    long count();

    long countByMemberBranch(Branch branch);

    long countByActiveTrueAndMemberBranch(Branch branch);

    long countByActiveTrue();

    boolean existsByMemberId(String memberId);
}
